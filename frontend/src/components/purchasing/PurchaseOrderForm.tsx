import React from 'react';
import { useForm, useFieldArray, useWatch } from 'react-hook-form';
import type { SubmitHandler } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import type { CreatePORequest } from '../../api/purchasing';
import { createPurchaseOrder } from '../../api/purchasing';
import { productApi } from '../../api/catalog';
import type { Product } from '../../types';
import { SupplierSelect } from './SupplierSelect';

interface PurchaseOrderFormProps {
  onCreated?: () => void;
}

// Validation schema
const poItemSchema = z.object({
  productId: z.string().min(1, 'Product is required'),
  quantity: z.number().int().min(1, 'Quantity must be at least 1'),
  unitCost: z.string().min(1, 'Unit cost is required'),
});

const purchaseOrderSchema = z.object({
  supplierId: z.string().min(1, 'Supplier is required'),
  notes: z.string().max(1000, 'Notes must not exceed 1000 characters').optional(),
  expectedDeliveryDate: z.string().optional(),
  items: z.array(poItemSchema).min(1, 'At least one item is required'),
});

type PurchaseOrderFormData = z.infer<typeof purchaseOrderSchema>;

/**
 * PurchaseOrderForm - Create a new purchase order
 * Allows selecting supplier and adding items
 */
export const PurchaseOrderForm: React.FC<PurchaseOrderFormProps> = ({
  onCreated,
}) => {
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = React.useState(false);
  const [products, setProducts] = React.useState<Product[]>([]);
  const [productsLoaded, setProductsLoaded] = React.useState(false);

  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
    watch,
  } = useForm<PurchaseOrderFormData>({
    resolver: zodResolver(purchaseOrderSchema),
    defaultValues: {
      supplierId: '',
      notes: '',
      expectedDeliveryDate: '',
      items: [{ productId: '', quantity: 1, unitCost: '0' }],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'items',
  });

  const supplierId = watch('supplierId');
  const items = useWatch({ control, name: 'items' });

  // Load products on mount
  React.useEffect(() => {
    const loadProducts = async () => {
      try {
        const response = await productApi.getAll(0);
        setProducts(response.content);
        setProductsLoaded(true);
      } catch (error) {
        console.error('Failed to load products:', error);
        setProductsLoaded(true);
      }
    };

    if (!productsLoaded) {
      loadProducts();
    }
  }, [productsLoaded]);

  const onSubmit: SubmitHandler<PurchaseOrderFormData> = async (data) => {
    setIsSubmitting(true);
    setSubmitError(null);
    setSubmitSuccess(false);

    try {
      const poData: CreatePORequest = {
        supplierId: Number(data.supplierId),
        notes: data.notes || undefined,
        expectedDeliveryDate: data.expectedDeliveryDate || undefined,
        items: data.items.map((item) => ({
          productId: Number(item.productId),
          quantity: item.quantity,
          unitCost: item.unitCost,
        })),
      };

      await createPurchaseOrder(poData);

      setSubmitSuccess(true);
      onCreated?.();

      // Clear success message after 3 seconds
      setTimeout(() => setSubmitSuccess(false), 3000);
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : 'Failed to create purchase order'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // Calculate totals
  const calculateTotal = () => {
    return items.reduce((total, item) => {
      const qty = item.quantity || 0;
      const cost = parseFloat(item.unitCost || '0');
      return total + qty * cost;
    }, 0);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div>
        <h3 className="text-lg font-medium" style={{ color: '#1F2937' }}>
          Create Purchase Order
        </h3>
      </div>

      {/* Supplier selection */}
      <SupplierSelect
        value={supplierId}
        onChange={(id) => {
          setValue('supplierId', id, { shouldValidate: true });
        }}
      />
      {errors.supplierId && (
        <p className="text-sm" style={{ color: '#FF2E21' }}>{errors.supplierId.message}</p>
      )}

      {/* Order Metadata */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium mb-1" style={{ color: '#1F2937' }}>
            Fecha de Entrega Esperada
          </label>
          <input
            type="date"
            {...register('expectedDeliveryDate')}
            className="block w-full px-3 py-2 rounded-md focus:outline-none"
            style={{
              border: '1px solid #E8E3DB',
              color: '#1F2937',
              backgroundColor: '#FFFFFF',
            }}
            onFocus={(e) => {
              e.currentTarget.style.borderColor = '#038E57';
            }}
            onBlur={(e) => {
              e.currentTarget.style.borderColor = '#E8E3DB';
            }}
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1" style={{ color: '#1F2937' }}>
            Notas
          </label>
          <input
            type="text"
            {...register('notes')}
            placeholder="Notas adicionales para la orden..."
            className="block w-full px-3 py-2 rounded-md focus:outline-none"
            style={{
              border: '1px solid #E8E3DB',
              color: '#1F2937',
              backgroundColor: '#FFFFFF',
            }}
            onFocus={(e) => {
              e.currentTarget.style.borderColor = '#038E57';
            }}
            onBlur={(e) => {
              e.currentTarget.style.borderColor = '#E8E3DB';
            }}
          />
          {errors.notes && (
            <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>{errors.notes.message}</p>
          )}
        </div>
      </div>

      {/* Items section */}
      <div>
        <h4 className="text-base font-medium" style={{ color: '#1F2937' }}>
          Items
        </h4>

        <div className="space-y-3">
          {fields.map((field, index) => (
            <div
              key={field.id}
              className="p-4 rounded-lg space-y-3"
              style={{ border: '1px solid #E8E3DB' }}
            >
              {/* Product select */}
              <div>
                <label className="block text-sm font-medium" style={{ color: '#1F2937' }}>
                  Product
                </label>
                <select
                  {...register(`items.${index}.productId` as const)}
                   className="block w-full px-3 py-2 rounded-md focus:outline-none"
                   style={{
                     border: '1px solid #E8E3DB',
                     color: '#1F2937',
                     backgroundColor: '#FFFFFF',
                   }}
                   onFocus={(e) => {
                     e.currentTarget.style.borderColor = '#038E57';
                   }}
                   onBlur={(e) => {
                     e.currentTarget.style.borderColor = '#E8E3DB';
                   }}
                 >
                  <option value="">Select a product</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name} (${product.salePrice})
                    </option>
                  ))}
                </select>
                {errors.items?.[index]?.productId && (
                  <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>
                    {errors.items[index]?.productId?.message}
                  </p>
                )}
              </div>

              {/* Quantity and Unit Cost */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium" style={{ color: '#1F2937' }}>
                    Quantity
                  </label>
                  <input
                     type="number"
                     {...register(`items.${index}.quantity` as const, {
                       valueAsNumber: true,
                     })}
                     className="w-full px-3 py-2 rounded-md focus:outline-none"
                     style={{
                       border: '1px solid #E8E3DB',
                       color: '#1F2937',
                       backgroundColor: '#FFFFFF',
                       fontVariantNumeric: 'tabular-nums',
                     }}
                     placeholder="0"
                     onFocus={(e) => {
                       e.currentTarget.style.borderColor = '#038E57';
                     }}
                     onBlur={(e) => {
                       e.currentTarget.style.borderColor = '#E8E3DB';
                     }}
                   />
                  {errors.items?.[index]?.quantity && (
                    <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>
                      {errors.items[index]?.quantity?.message}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium" style={{ color: '#1F2937' }}>
                    Unit Cost
                  </label>
                  <input
                     type="text"
                     {...register(`items.${index}.unitCost` as const)}
                     className="w-full px-3 py-2 rounded-md focus:outline-none"
                     style={{
                       border: '1px solid #E8E3DB',
                       color: '#1F2937',
                       backgroundColor: '#FFFFFF',
                       fontVariantNumeric: 'tabular-nums',
                     }}
                     placeholder="0.00"
                     onFocus={(e) => {
                       e.currentTarget.style.borderColor = '#038E57';
                     }}
                     onBlur={(e) => {
                       e.currentTarget.style.borderColor = '#E8E3DB';
                     }}
                   />
                  {errors.items?.[index]?.unitCost && (
                    <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>
                      {errors.items[index]?.unitCost?.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Remove button */}
              {fields.length > 1 && (
                <button
                   type="button"
                   onClick={() => remove(index)}
                   className="text-sm px-3 py-2 rounded-md font-medium transition-colors"
                   style={{
                     backgroundColor: '#FF2E21',
                     color: '#FFFFFF',
                   }}
                   onMouseEnter={(e) => {
                     e.currentTarget.style.opacity = '0.9';
                   }}
                   onMouseLeave={(e) => {
                     e.currentTarget.style.opacity = '1';
                   }}
                 >
                   Remove Item
                 </button>
              )}
            </div>
          ))}
        </div>

        {/* Add item button */}
        <button
           type="button"
           onClick={() =>
             append({ productId: '', quantity: 1, unitCost: '0' })
           }
           className="text-sm px-3 py-2 rounded-md font-medium transition-colors mt-3"
           style={{
             backgroundColor: '#FFAC00',
             color: '#FFFFFF',
           }}
           onMouseEnter={(e) => {
             e.currentTarget.style.opacity = '0.9';
           }}
           onMouseLeave={(e) => {
             e.currentTarget.style.opacity = '1';
           }}
         >
           + Add Item
         </button>

        {errors.items && (
          <p className="mt-2 text-sm" style={{ color: '#FF2E21' }}>{errors.items.message}</p>
        )}
      </div>

      {/* Total */}
      <div className="p-4 rounded-lg" style={{ backgroundColor: '#FEFAF1', border: '1px solid #E8E3DB' }}>
        <div className="flex justify-between items-center">
          <span className="text-lg font-semibold" style={{ color: '#1F2937' }}>Total:</span>
          <span className="text-2xl font-bold" style={{ color: '#038E57', fontVariantNumeric: 'tabular-nums' }}>
            ${calculateTotal().toFixed(2)}
          </span>
        </div>
      </div>

      {/* Error message */}
      {submitError && (
        <div className="p-4 rounded-md" style={{ backgroundColor: 'rgba(255,46,33,0.1)', border: '1px solid #FF2E21' }}>
          <p className="text-sm" style={{ color: '#FF2E21' }}>{submitError}</p>
        </div>
      )}

      {/* Success message */}
      {submitSuccess && (
        <div className="p-4 rounded-md" style={{ backgroundColor: '#E8F4F0', border: '1px solid #10B981' }}>
          <p className="text-sm" style={{ color: '#10B981' }}>
            Purchase order created successfully!
          </p>
        </div>
      )}

      {/* Submit button */}
       <button
         type="submit"
         disabled={isSubmitting}
         className="w-full px-3 py-2 rounded-md font-medium transition-colors text-white"
         style={{
           backgroundColor: isSubmitting ? '#CCCCCC' : '#038E57',
           cursor: isSubmitting ? 'not-allowed' : 'pointer',
         }}
         onMouseEnter={(e) => {
           if (!isSubmitting) e.currentTarget.style.opacity = '0.9';
         }}
         onMouseLeave={(e) => {
           if (!isSubmitting) e.currentTarget.style.opacity = '1';
         }}
       >
         {isSubmitting ? 'Creating...' : 'Create Purchase Order'}
       </button>
    </form>
  );
};
