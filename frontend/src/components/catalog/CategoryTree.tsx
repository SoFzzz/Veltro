import { useState } from 'react';
import type { Category } from '../../types';

interface CategoryTreeProps {
  categories: Category[];
  selectedId?: number;
  onSelect?: (category: Category) => void;
  onEdit?: (category: Category) => void;
  onDelete?: (category: Category) => void;
}

interface CategoryNodeProps {
  category: Category;
  level: number;
  selectedId?: number;
  onSelect?: (category: Category) => void;
  onEdit?: (category: Category) => void;
  onDelete?: (category: Category) => void;
}

function CategoryNode({
  category,
  level,
  selectedId,
  onSelect,
  onEdit,
  onDelete,
}: CategoryNodeProps) {
  const [isExpanded, setIsExpanded] = useState(true);
  const hasChildren = category.children && category.children.length > 0;
  const isSelected = selectedId === category.id;

  return (
    <div role="treeitem" aria-expanded={hasChildren ? isExpanded : undefined}>
      <div
        className="flex items-center justify-between py-2 px-3 rounded-md cursor-pointer"
        style={{
          marginLeft: `${level * 20}px`,
          backgroundColor: isSelected ? '#E8F4F0' : 'transparent',
          border: isSelected ? '1px solid #038E57' : 'none',
          color: isSelected ? '#038E57' : '#1F2937',
        }}
        onClick={() => onSelect?.(category)}
        onMouseEnter={(e) => {
          if (!isSelected) e.currentTarget.style.backgroundColor = '#F9F7F2';
        }}
        onMouseLeave={(e) => {
          if (!isSelected) e.currentTarget.style.backgroundColor = 'transparent';
        }}
        role="button"
        tabIndex={0}
        aria-selected={isSelected}
        aria-label={`${category.name}${!category.active ? ' (Inactive)' : ''}`}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onSelect?.(category);
          }
          if (e.key === 'ArrowRight' && hasChildren && !isExpanded) {
            setIsExpanded(true);
          }
          if (e.key === 'ArrowLeft' && hasChildren && isExpanded) {
            setIsExpanded(false);
          }
        }}
        title={`Select ${category.name}${!category.active ? ' (Inactive)' : ''}`}
      >
        <div className="flex items-center">
          {hasChildren && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                setIsExpanded(!isExpanded);
              }}
              className="mr-2 transition-colors"
              style={{ color: '#038E57' }}
              aria-label={isExpanded ? `Collapse ${category.name}` : `Expand ${category.name}`}
              title={isExpanded ? 'Collapse category' : 'Expand category'}
              aria-expanded={isExpanded}
              onMouseEnter={(e) => {
                e.currentTarget.style.opacity = '0.7';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.opacity = '1';
              }}
            >
              {isExpanded ? '▼' : '▶'}
            </button>
          )}
          {!hasChildren && <span className="w-4 mr-2" aria-hidden="true" />}
          <span className={`text-sm ${isSelected ? 'font-medium' : ''}`} style={{ color: isSelected ? '#038E57' : '#1F2937' }}>
            {category.name}
          </span>
          {!category.active && (
            <span className="ml-2 px-1.5 py-0.5 text-xs rounded" style={{ backgroundColor: '#F3F4F6', color: '#6B7280' }} aria-label="Category is inactive">
              Inactivo
            </span>
          )}
        </div>
        <div className="flex space-x-2">
          {onEdit && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onEdit(category);
              }}
              className="text-sm transition-colors"
              style={{ color: '#038E57' }}
              aria-label={`Edit ${category.name}`}
              title={`Edit ${category.name}`}
              onMouseEnter={(e) => {
                e.currentTarget.style.opacity = '0.7';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.opacity = '1';
              }}
            >
              Editar
            </button>
          )}
          {onDelete && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDelete(category);
              }}
              className="text-sm transition-colors"
              style={{ color: '#FF2E21' }}
              aria-label={`Delete ${category.name}`}
              title={`Delete ${category.name}`}
              onMouseEnter={(e) => {
                e.currentTarget.style.opacity = '0.7';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.opacity = '1';
              }}
            >
              Eliminar
            </button>
          )}
        </div>
      </div>
      {hasChildren && isExpanded && (
        <div role="group">
          {category.children!.map((child) => (
            <CategoryNode
              key={child.id}
              category={child}
              level={level + 1}
              selectedId={selectedId}
              onSelect={onSelect}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export function CategoryTree({
  categories,
  selectedId,
  onSelect,
  onEdit,
  onDelete,
}: CategoryTreeProps) {
  if (categories.length === 0) {
    return (
      <div className="text-center py-8" style={{ color: '#6B7280' }} aria-label="No categories registered">
        No hay categorías registradas
      </div>
    );
  }

  return (
    <div className="space-y-1" role="tree" aria-label="Category hierarchy">
      {categories.map((category) => (
        <CategoryNode
          key={category.id}
          category={category}
          level={0}
          selectedId={selectedId}
          onSelect={onSelect}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      ))}
    </div>
  );
}
