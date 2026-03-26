import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import type { SubmitHandler } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import type { AlertConfig } from '../../api/inventory';
import { updateAlertConfig } from '../../api/inventory';

interface AlertConfigFormProps {
  productId: number;
  productName: string;
  initialConfig: AlertConfig;
  onSaved?: () => void;
}

// Validation schema
const alertConfigSchema = z.object({
  criticalStock: z.number().int().min(0, 'Must be 0 or greater'),
  minStock: z.number().int().min(0, 'Must be 0 or greater'),
  overstockThreshold: z.number().int().min(0, 'Must be 0 or greater'),
});

type AlertConfigFormData = z.infer<typeof alertConfigSchema>;

/**
 * AlertConfigForm - Configure alert thresholds for a product
 * Allows setting critical stock, min stock, and overstock threshold
 */
export const AlertConfigForm: React.FC<AlertConfigFormProps> = ({
  productId,
  productName,
  initialConfig,
  onSaved,
}) => {
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = React.useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<AlertConfigFormData>({
    resolver: zodResolver(alertConfigSchema),
    defaultValues: {
      criticalStock: initialConfig.criticalStock,
      minStock: initialConfig.minStock,
      overstockThreshold: initialConfig.overstockThreshold,
    },
  });

  useEffect(() => {
    reset({
      criticalStock: initialConfig.criticalStock,
      minStock: initialConfig.minStock,
      overstockThreshold: initialConfig.overstockThreshold,
    });
  }, [initialConfig, reset]);

  const onSubmit: SubmitHandler<AlertConfigFormData> = async (data) => {
    setIsSubmitting(true);
    setSubmitError(null);
    setSubmitSuccess(false);

    try {
      const config = {
        criticalStock: data.criticalStock,
        minStock: data.minStock,
        overstockThreshold: data.overstockThreshold,
      };

      await updateAlertConfig(productId, config);

      setSubmitSuccess(true);
      onSaved?.();

      // Clear success message after 3 seconds
      setTimeout(() => setSubmitSuccess(false), 3000);
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : 'Failed to save configuration'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div>
        <h3 className="text-lg font-medium mb-4" style={{ color: '#1F2937' }}>
          Alert Configuration for {productName}
        </h3>
      </div>

      {/* Critical Stock */}
      <div>
        <label
          htmlFor="criticalStock"
          className="block text-sm font-medium mb-1"
          style={{ color: '#1F2937' }}
        >
          Critical Stock Level (Alert when ≤ this quantity)
        </label>
        <input
          id="criticalStock"
          type="number"
          {...register('criticalStock')}
          className="block w-full px-3 py-2 rounded-md focus:outline-none border-2 transition"
          style={{
            backgroundColor: '#FFFFFF',
            borderColor: '#E8E3DB',
            color: '#1F2937',
          }}
          onFocus={(e) => {
            (e.currentTarget as HTMLInputElement).style.borderColor = '#038E57';
          }}
          onBlur={(e) => {
            (e.currentTarget as HTMLInputElement).style.borderColor = '#E8E3DB';
          }}
          placeholder="0"
        />
        {errors.criticalStock && (
          <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>
            {errors.criticalStock.message}
          </p>
        )}
      </div>

      {/* Min Stock */}
      <div>
        <label
          htmlFor="minStock"
          className="block text-sm font-medium mb-1"
          style={{ color: '#1F2937' }}
        >
          Minimum Stock Level (Warning when ≤ this quantity)
        </label>
        <input
          id="minStock"
          type="number"
          {...register('minStock')}
          className="block w-full px-3 py-2 rounded-md focus:outline-none border-2 transition"
          style={{
            backgroundColor: '#FFFFFF',
            borderColor: '#E8E3DB',
            color: '#1F2937',
          }}
          onFocus={(e) => {
            (e.currentTarget as HTMLInputElement).style.borderColor = '#038E57';
          }}
          onBlur={(e) => {
            (e.currentTarget as HTMLInputElement).style.borderColor = '#E8E3DB';
          }}
          placeholder="0"
        />
        {errors.minStock && (
          <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>
            {errors.minStock.message}
          </p>
        )}
      </div>

      {/* Overstock Threshold */}
      <div>
        <label
          htmlFor="overstockThreshold"
          className="block text-sm font-medium mb-1"
          style={{ color: '#1F2937' }}
        >
          Overstock Threshold (Alert when ≥ this quantity)
        </label>
        <input
          id="overstockThreshold"
          type="number"
          {...register('overstockThreshold')}
          className="block w-full px-3 py-2 rounded-md focus:outline-none border-2 transition"
          style={{
            backgroundColor: '#FFFFFF',
            borderColor: '#E8E3DB',
            color: '#1F2937',
          }}
          onFocus={(e) => {
            (e.currentTarget as HTMLInputElement).style.borderColor = '#038E57';
          }}
          onBlur={(e) => {
            (e.currentTarget as HTMLInputElement).style.borderColor = '#E8E3DB';
          }}
          placeholder="0"
        />
        {errors.overstockThreshold && (
          <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>
            {errors.overstockThreshold.message}
          </p>
        )}
      </div>

      {/* Error message */}
      {submitError && (
        <div
          className="p-4 rounded-md border-2"
          style={{
            backgroundColor: 'rgba(255, 46, 33, 0.1)',
            borderColor: '#FF2E21',
          }}
        >
          <p style={{ color: '#FF2E21', fontSize: '0.875rem' }}>{submitError}</p>
        </div>
      )}

      {/* Success message */}
      {submitSuccess && (
        <div
          className="p-4 rounded-md border-2"
          style={{
            backgroundColor: '#E8F4F0',
            borderColor: '#10B981',
          }}
        >
          <p style={{ color: '#038E57', fontSize: '0.875rem' }}>
            Configuration saved successfully!
          </p>
        </div>
      )}

      {/* Submit button */}
      <button
        type="submit"
        disabled={isSubmitting}
        className="w-full px-4 py-2 font-medium rounded-md transition"
        style={{
          backgroundColor: '#038E57',
          color: '#FFFFFF',
          opacity: isSubmitting ? 0.6 : 1,
          cursor: isSubmitting ? 'not-allowed' : 'pointer',
        }}
        onMouseEnter={(e) => {
          if (!isSubmitting) {
            (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#10A96D';
          }
        }}
        onMouseLeave={(e) => {
          (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#038E57';
        }}
      >
        {isSubmitting ? 'Saving...' : 'Save Configuration'}
      </button>
    </form>
  );
};
