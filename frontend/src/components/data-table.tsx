import { useTable, type RowData } from '@tanstack/react-table'

import { dataTableFeatures, type DataColumnDef } from '@/components/data-table-config'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

type DataTableProps<T extends RowData> = {
  columns: DataColumnDef<T>[]
  data: T[]
  emptyMessage: string
}

export function DataTable<T extends RowData>({ columns, data, emptyMessage }: DataTableProps<T>) {
  const table = useTable({
    features: dataTableFeatures,
    columns,
    data,
  })

  return (
    <div className="overflow-hidden rounded-xl border bg-white">
      <Table>
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <TableHead key={header.id}>
                  {header.isPlaceholder
                    ? null
                    : <table.FlexRender header={header} />}
                </TableHead>
              ))}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {table.getRowModel().rows.length > 0 ? (
            table.getRowModel().rows.map((row) => (
              <TableRow key={row.id}>
                {row.getAllCells().map((cell) => (
                  <TableCell key={cell.id}>
                    <table.FlexRender cell={cell} />
                  </TableCell>
                ))}
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell className="h-32 text-center text-muted-foreground" colSpan={columns.length}>
                {emptyMessage}
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  )
}
