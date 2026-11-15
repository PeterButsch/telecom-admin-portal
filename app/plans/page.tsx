"use client"

import { useMemo, useState } from "react"
import { Loader2, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { FilterSelect } from "@/components/filter-select"
import { PageShell } from "@/components/page-shell"
import { PlanFormDialog } from "@/components/plan-form-dialog"
import { PlansTable } from "@/components/plans-table"
import {
  BRANDS,
  PLAN_CATEGORIES,
  SEGMENTS,
  usePlans,
  type Brand,
  type Plan,
  type PlanCategory,
  type PlanRequest,
  type Segment,
} from "@/hooks/use-plans"
import { options } from "@/lib/labels"

export default function PlansPage() {
  const { plans, loading, error, createPlan, updatePlan, deletePlan } = usePlans()
  const [brand, setBrand] = useState<Brand | null>(null)
  const [category, setCategory] = useState<PlanCategory | null>(null)
  const [segment, setSegment] = useState<Segment | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editingPlan, setEditingPlan] = useState<Plan | null>(null)
  const [planToDelete, setPlanToDelete] = useState<Plan | null>(null)

  const shown = useMemo(
    () =>
      plans.filter(
        (p) =>
          (!brand || p.brand === brand) &&
          (!category || p.category === category) &&
          (!segment || p.segment === segment)
      ),
    [plans, brand, category, segment]
  )

  const handleFormSubmit = async (data: PlanRequest) => {
    if (editingPlan) {
      await updatePlan(editingPlan.id, data)
    } else {
      await createPlan(data)
    }
  }

  return (
    <PageShell title="Plans">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-headline-2">Plan Catalogue</CardTitle>
            <div className="flex items-center gap-4">
              <span className="text-muted-foreground text-sm">
                {loading ? "Loading..." : `${shown.length} of ${plans.length} plans`}
              </span>
              <Button
                onClick={() => {
                  setEditingPlan(null)
                  setFormOpen(true)
                }}
              >
                <Plus className="mr-2 h-4 w-4" />
                Add Plan
              </Button>
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <FilterSelect
              id="brand-filter"
              label="Brand"
              value={brand}
              options={options(BRANDS)}
              onChange={(v) => setBrand(v as Brand | null)}
            />
            <FilterSelect
              id="category-filter"
              label="Category"
              value={category}
              options={options(PLAN_CATEGORIES)}
              onChange={(v) => setCategory(v as PlanCategory | null)}
            />
            <FilterSelect
              id="segment-filter"
              label="Segment"
              value={segment}
              options={options(SEGMENTS)}
              onChange={(v) => setSegment(v as Segment | null)}
            />
          </div>
        </CardHeader>
        <CardContent className="flex-1 overflow-hidden">
          {loading ? (
            <div className="text-muted-foreground flex h-full items-center justify-center">
              <Loader2 className="mr-2 h-6 w-6 animate-spin" />
              Loading...
            </div>
          ) : error ? (
            <div role="alert" className="text-destructive py-8 text-center">
              {error}
            </div>
          ) : (
            <div className="h-full overflow-auto">
              <PlansTable
                plans={shown}
                onEdit={(plan) => {
                  setEditingPlan(plan)
                  setFormOpen(true)
                }}
                onDelete={setPlanToDelete}
              />
            </div>
          )}
        </CardContent>
      </Card>

      <PlanFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        plan={editingPlan}
        onSubmit={handleFormSubmit}
      />

      <ConfirmDialog
        open={planToDelete !== null}
        onOpenChange={(open) => !open && setPlanToDelete(null)}
        title="Delete Plan"
        description={
          <>
            Delete &quot;{planToDelete?.name}&quot;? Plans that have ever had a line cannot be
            deleted; mark them inactive instead.
          </>
        }
        onConfirm={async () => {
          if (planToDelete) await deletePlan(planToDelete.id)
        }}
      />
    </PageShell>
  )
}
