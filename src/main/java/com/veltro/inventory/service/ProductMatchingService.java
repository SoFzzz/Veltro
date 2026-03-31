package com.veltro.inventory.service;

import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches AI-generated product suggestions against the active catalog of the current business.
 *
 * <p>The algorithm is intentionally conservative: it only auto-links when there is a clear
 * best candidate, and falls back to an empty result when the suggestion is ambiguous.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMatchingService {

    private static final Pattern VOLUME_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(ml|l)\\b");

    private final ProductRepository productRepository;

    public Optional<ProductEntity> findMatch(String suggestedName, Long businessId) {
        String normalizedSuggestion = normalize(suggestedName);
        if (normalizedSuggestion.isBlank()) {
            return Optional.empty();
        }

        String keyword = extractKeyword(normalizedSuggestion);
        if (keyword.isBlank()) {
            return Optional.empty();
        }

        List<ProductEntity> candidates =
                productRepository.findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(businessId, keyword);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Optional<ProductEntity> exactMatch = candidates.stream()
                .filter(product -> normalize(product.getName()).equals(normalizedSuggestion))
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        if (candidates.size() == 1) {
            return Optional.of(candidates.get(0));
        }

        Optional<String> suggestionVolume = extractVolume(normalizedSuggestion);
        if (suggestionVolume.isPresent()) {
            List<ProductEntity> volumeMatches = candidates.stream()
                    .filter(product -> extractVolume(normalize(product.getName()))
                            .map(suggestionVolume.get()::equals)
                            .orElse(false))
                    .toList();

            if (volumeMatches.size() == 1) {
                return Optional.of(volumeMatches.get(0));
            }
            if (volumeMatches.size() > 1) {
                return selectUniqueBest(normalizedSuggestion, volumeMatches);
            }
        }

        return selectUniqueBest(normalizedSuggestion, candidates);
    }

    private Optional<ProductEntity> selectUniqueBest(String normalizedSuggestion, List<ProductEntity> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        List<ScoredCandidate> scored = candidates.stream()
                .map(candidate -> new ScoredCandidate(candidate, score(normalizedSuggestion, normalize(candidate.getName()))))
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
                .toList();

        if (scored.isEmpty() || scored.get(0).score() <= 0) {
            return Optional.empty();
        }

        if (scored.size() > 1 && scored.get(0).score() == scored.get(1).score()) {
            log.info("AI product suggestion '{}' is ambiguous across {} candidates", normalizedSuggestion, candidates.size());
            return Optional.empty();
        }

        return Optional.of(scored.get(0).product());
    }

    private int score(String normalizedSuggestion, String normalizedCandidate) {
        int score = 0;

        if (normalizedCandidate.equals(normalizedSuggestion)) {
            score += 1000;
        }
        if (normalizedCandidate.contains(normalizedSuggestion)) {
            score += 200;
        }
        if (normalizedSuggestion.contains(normalizedCandidate)) {
            score += 100;
        }

        List<String> tokens = tokenize(normalizedSuggestion);
        for (String token : tokens) {
            if (normalizedCandidate.contains(token)) {
                score += 25;
            }
        }

        Optional<String> suggestionVolume = extractVolume(normalizedSuggestion);
        Optional<String> candidateVolume = extractVolume(normalizedCandidate);
        if (suggestionVolume.isPresent() && suggestionVolume.equals(candidateVolume)) {
            score += 250;
        }

        return score;
    }

    private String extractKeyword(String normalizedSuggestion) {
        return tokenize(normalizedSuggestion).stream()
                .findFirst()
                .orElse("");
    }

    private List<String> tokenize(String value) {
        return List.of(value.split("\\s+")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() > 1)
                .filter(token -> !isGenericToken(token))
                .toList();
    }

    private boolean isGenericToken(String token) {
        return switch (token) {
            case "ml", "l", "lt", "lts", "botella", "sabor", "bebida", "gaseosa", "gasificada" -> true;
            default -> false;
        };
    }

    private Optional<String> extractVolume(String normalizedValue) {
        Matcher matcher = VOLUME_PATTERN.matcher(normalizedValue);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String amount = matcher.group(1).replace(',', '.');
        String unit = matcher.group(2);
        return Optional.of(amount + unit);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s.]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized;
    }

    private record ScoredCandidate(ProductEntity product, int score) {
    }
}
