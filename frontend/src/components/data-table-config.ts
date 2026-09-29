import { tableFeatures, type ColumnDef, type RowData } from '@tanstack/react-table'

export const dataTableFeatures = tableFeatures({})

export type DataColumnDef<T extends RowData> = ColumnDef<typeof dataTableFeatures, T, unknown>
