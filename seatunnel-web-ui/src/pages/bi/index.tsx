import {
  BarChart3,
  Check,
  Edit3,
  LayoutDashboard,
  Plus,
  RefreshCw,
  Settings,
  Sparkles,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import GridLayout, { Layout } from "react-grid-layout";

import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";
import "./index.less";

import ChartCard from "./components/ChartCard";

import {
  COLS,
  MARGIN,
  ROW_HEIGHT,
  defaultChartOption,
} from "./dashboard.data";
import {
  getCanvasHeight,
  getInitialLayout,
  getLayoutBottomRow,
  persistDashboardLayout,
} from "./dashboard.utils";
import CreateChartModal from "./components/CreateChartModal";

interface ChartMeta {
  id: string;
  name: string;
  config: CreateChartValues;
}

interface DashboardItem {
  id: string;
  name: string;
  layout: Layout[];
  charts: Record<string, ChartMeta>;
}

const dashboardTheme = {
  primaryText: "text-[hsl(231_48%_48%)]",
  primaryBg: "bg-[hsl(231_48%_48%)]",
  primaryBgHover: "hover:bg-[hsl(231_52%_43%)]",

  primarySoftBg: "bg-[hsl(231_48%_48%/0.08)]",
  primarySoftBgHover: "hover:bg-[hsl(231_48%_48%/0.12)]",

  primaryBorder: "border-[hsl(231_48%_48%)]",
  primarySoftBorder: "border-[hsl(231_48%_48%/0.22)]",
  primarySoftBorderHover: "hover:border-[hsl(231_48%_48%/0.34)]",

  primaryRingShadow:
    "shadow-[0_0_0_1px_hsl(231_48%_48%),0_10px_30px_hsl(231_48%_48%/0.14),0_0_0_4px_hsl(231_48%_48%/0.08)]",

  primaryHoverShadow:
    "hover:shadow-[0_0_0_1px_hsl(231_48%_48%/0.14),0_10px_28px_hsl(231_48%_48%/0.10)]",
};

const createDashboardId = () => {
  return `dashboard_${Date.now()}`;
};

const createChartId = () => {
  return `chart_${Date.now()}`;
};

const Dashboard = () => {
  const canvasInnerRef = useRef<HTMLDivElement>(null);

  const [dashboards, setDashboards] = useState<DashboardItem[]>([
    {
      id: "default",
      name: "asdf",
      layout: getInitialLayout(),
      charts: {},
    },
  ]);

  const [activeDashboardId, setActiveDashboardId] = useState("default");
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const [canvasWidth, setCanvasWidth] = useState(1200);
  const [isEditing, setIsEditing] = useState(false);
  const [isInteracting, setIsInteracting] = useState(false);
  const [createChartOpen, setCreateChartOpen] = useState(false);

  const activeDashboard = useMemo(() => {
    return dashboards.find((item) => item.id === activeDashboardId);
  }, [dashboards, activeDashboardId]);

  const layout = activeDashboard?.layout || [];
  const charts = activeDashboard?.charts || {};

  const dynamicRowCount = useMemo(() => {
    return getLayoutBottomRow(layout) + 6;
  }, [layout]);

  const canvasMinHeight = useMemo(() => {
    return getCanvasHeight(dynamicRowCount);
  }, [dynamicRowCount]);

  useEffect(() => {
    if (!canvasInnerRef.current) {
      return;
    }

    let animationFrameId = 0;

    const updateWidth = () => {
      window.cancelAnimationFrame(animationFrameId);

      animationFrameId = window.requestAnimationFrame(() => {
        if (!canvasInnerRef.current) {
          return;
        }

        setCanvasWidth(canvasInnerRef.current.clientWidth);
      });
    };

    updateWidth();

    const observer = new ResizeObserver(updateWidth);
    observer.observe(canvasInnerRef.current);

    return () => {
      window.cancelAnimationFrame(animationFrameId);
      observer.disconnect();
    };
  }, []);

  const updateActiveLayout = useCallback(
    (nextLayout: Layout[], shouldPersist = false) => {
      setDashboards((prev) => {
        return prev.map((dashboard) => {
          if (dashboard.id !== activeDashboardId) {
            return dashboard;
          }

          return {
            ...dashboard,
            layout: nextLayout,
          };
        });
      });

      if (shouldPersist) {
        persistDashboardLayout(nextLayout);
      }
    },
    [activeDashboardId]
  );

  const handleCreateDashboard = () => {
    const nextDashboard: DashboardItem = {
      id: createDashboardId(),
      name: `新仪表盘 ${dashboards.length + 1}`,
      layout: [],
      charts: {},
    };

    setDashboards((prev) => [...prev, nextDashboard]);
    setActiveDashboardId(nextDashboard.id);
    setSelectedCardId(null);
    setIsEditing(true);
    setIsInteracting(false);
  };

  const handleOpenCreateChart = () => {
    setCreateChartOpen(true);
  };

  const handleCreateChart = (values: CreateChartValues) => {
    const nextChartId = createChartId();
    const bottomRow = getLayoutBottomRow(layout);

    const nextLayoutItem: Layout = {
      i: nextChartId,
      x: 0,
      y: bottomRow,
      w: 6,
      h: 5,
      minW: 3,
      minH: 3,
    };

    const nextChartMeta: ChartMeta = {
      id: nextChartId,
      name: values.name,
      config: values,
    };

    setDashboards((prev) => {
      return prev.map((dashboard) => {
        if (dashboard.id !== activeDashboardId) {
          return dashboard;
        }

        const nextLayout = [...dashboard.layout, nextLayoutItem];

        persistDashboardLayout(nextLayout);

        return {
          ...dashboard,
          layout: nextLayout,
          charts: {
            ...dashboard.charts,
            [nextChartId]: nextChartMeta,
          },
        };
      });
    });

    setSelectedCardId(nextChartId);
    setIsEditing(true);
    setCreateChartOpen(false);
  };

  const handleChangeDashboard = (dashboardId: string) => {
    setActiveDashboardId(dashboardId);
    setSelectedCardId(null);
    setIsInteracting(false);
  };

  const handleToggleEdit = () => {
    setIsEditing((prev) => !prev);
    setSelectedCardId(null);
    setIsInteracting(false);
  };

  const handleRefresh = () => {
    console.log("refresh dashboard");
  };

  return (
    <>
      <div className="flex h-screen overflow-hidden bg-[#f7f7f8]">
        <aside className="flex w-[224px] shrink-0 flex-col border-r border-slate-200 bg-white">
          <div className="flex h-14 items-center gap-2 border-b border-slate-100 px-4">
            <div
              className={[
                "flex h-8 w-8 items-center justify-center rounded-xl",
                dashboardTheme.primarySoftBg,
                dashboardTheme.primaryText,
              ].join(" ")}
            >
              <LayoutDashboard size={17} />
            </div>

            <div className="min-w-0">
              <div className="text-sm font-semibold text-slate-900">仪表盘</div>
              <div className="text-xs text-slate-400">Dashboard</div>
            </div>
          </div>

          <div className="flex items-center justify-between px-3 pb-2 pt-4">
            <span className="text-xs font-medium text-slate-400">
              仪表盘列表
            </span>

            <button
              type="button"
              onClick={handleCreateDashboard}
              className={[
                "flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition",
                dashboardTheme.primarySoftBgHover,
                `hover:${dashboardTheme.primaryText}`,
              ].join(" ")}
              title="新建仪表盘"
            >
              <Plus size={15} />
            </button>
          </div>

          <div className="flex-1 space-y-1 overflow-auto px-2">
            {dashboards.map((dashboard) => {
              const active = dashboard.id === activeDashboardId;

              return (
                <button
                  key={dashboard.id}
                  type="button"
                  onClick={() => handleChangeDashboard(dashboard.id)}
                  className={[
                    "flex h-9 w-full items-center gap-2 rounded-xl px-3 text-left text-sm transition",
                    active
                      ? [
                          dashboardTheme.primarySoftBg,
                          dashboardTheme.primaryText,
                          "font-medium",
                        ].join(" ")
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                  ].join(" ")}
                >
                  <BarChart3 size={15} />
                  <span className="min-w-0 flex-1 truncate">
                    {dashboard.name}
                  </span>
                </button>
              );
            })}
          </div>

          <div className="border-t border-slate-100 p-3">
            <button
              type="button"
              className="flex h-9 w-full items-center gap-2 rounded-xl px-3 text-sm text-slate-500 transition hover:bg-slate-50 hover:text-slate-900"
            >
              <Settings size={15} />
              仪表盘设置
            </button>
          </div>
        </aside>

        <main className="flex min-w-0 flex-1 flex-col">
          <header className="flex h-14 shrink-0 items-center justify-between border-b border-slate-200 bg-white px-4">
            <div className="flex min-w-0 items-center gap-3">
              <div
                className={[
                  "flex h-9 w-9 items-center justify-center rounded-xl",
                  dashboardTheme.primarySoftBg,
                  dashboardTheme.primaryText,
                ].join(" ")}
              >
                <LayoutDashboard size={18} />
              </div>

              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <h1 className="truncate text-sm font-semibold text-slate-900">
                    {activeDashboard?.name || "未命名仪表盘"}
                  </h1>

                  {isEditing && (
                    <span
                      className={[
                        "rounded-full px-2 py-0.5 text-[11px] font-medium",
                        dashboardTheme.primarySoftBg,
                        dashboardTheme.primaryText,
                      ].join(" ")}
                    >
                      编辑中
                    </span>
                  )}
                </div>

                <div className="text-xs text-slate-400">
                  支持拖拽布局、缩放卡片和添加图表
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleRefresh}
                className="flex h-9 items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-900"
              >
                <RefreshCw size={15} />
                刷新
              </button>

              <button
                type="button"
                onClick={handleOpenCreateChart}
                className={[
                  "flex h-9 items-center gap-1.5 rounded-xl border px-3 text-sm font-medium transition",
                  dashboardTheme.primarySoftBorder,
                  dashboardTheme.primarySoftBorderHover,
                  dashboardTheme.primarySoftBg,
                  dashboardTheme.primarySoftBgHover,
                  dashboardTheme.primaryText,
                ].join(" ")}
              >
                <Plus size={15} />
                创建图表
              </button>

              <button
                type="button"
                onClick={handleToggleEdit}
                className={[
                  "flex h-9 items-center gap-1.5 rounded-xl px-3 text-sm font-medium transition",
                  isEditing
                    ? "bg-slate-900 text-white hover:bg-slate-800"
                    : [
                        dashboardTheme.primaryBg,
                        dashboardTheme.primaryBgHover,
                        "text-white",
                      ].join(" "),
                ].join(" ")}
              >
                {isEditing ? <Check size={15} /> : <Edit3 size={15} />}
                {isEditing ? "完成" : "编辑"}
              </button>
            </div>
          </header>

          <div className="min-h-0 flex-1 overflow-auto bg-[#f7f7f8] p-2">
            <div
              ref={canvasInnerRef}
              className="relative box-border w-full overflow-visible bg-[#f7f7f8]"
              style={{
                minHeight: canvasMinHeight,
              }}
              onMouseDown={(event) => {
                if (event.target === event.currentTarget) {
                  setSelectedCardId(null);
                }
              }}
            >
              {layout.length === 0 ? (
                <div className="flex h-[420px] items-center justify-center">
                  <div className="flex w-[360px] flex-col items-center rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-10 text-center shadow-sm">
                    <div
                      className={[
                        "mb-4 flex h-12 w-12 items-center justify-center rounded-2xl",
                        dashboardTheme.primarySoftBg,
                        dashboardTheme.primaryText,
                      ].join(" ")}
                    >
                      <Sparkles size={22} />
                    </div>

                    <div className="text-base font-semibold text-slate-900">
                      还没有图表
                    </div>

                    <div className="mt-2 text-sm leading-6 text-slate-500">
                      点击创建图表，添加你的第一个数据卡片。
                    </div>

                    <button
                      type="button"
                      onClick={handleOpenCreateChart}
                      className={[
                        "mt-5 flex h-9 items-center gap-1.5 rounded-xl px-4 text-sm font-medium text-white transition",
                        dashboardTheme.primaryBg,
                        dashboardTheme.primaryBgHover,
                      ].join(" ")}
                    >
                      <Plus size={15} />
                      创建图表
                    </button>
                  </div>
                </div>
              ) : (
                <GridLayout
                  className={[
                    "dashboard-layout relative z-10 min-h-[inherit]",
                    isInteracting ? "dashboard-layout-editing" : "",
                  ]
                    .filter(Boolean)
                    .join(" ")}
                  layout={layout}
                  cols={COLS}
                  rowHeight={ROW_HEIGHT}
                  width={canvasWidth}
                  margin={MARGIN}
                  containerPadding={[0, 0]}
                  draggableHandle=".drag-handle"
                  draggableCancel=".dashboard-card-more"
                  resizeHandles={["se"]}
                  compactType={null}
                  preventCollision={false}
                  useCSSTransforms
                  isDraggable={isEditing}
                  isResizable={isEditing}
                  isBounded
                  onDragStart={(_, oldItem) => {
                    setIsInteracting(true);
                    setSelectedCardId(oldItem.i);
                  }}
                  onResizeStart={(_, oldItem) => {
                    setIsInteracting(true);
                    setSelectedCardId(oldItem.i);
                  }}
                  onDrag={(nextLayout) => {
                    updateActiveLayout(nextLayout);
                  }}
                  onResize={(nextLayout) => {
                    updateActiveLayout(nextLayout);
                  }}
                  onDragStop={(nextLayout) => {
                    setIsInteracting(false);
                    updateActiveLayout(nextLayout, true);
                  }}
                  onResizeStop={(nextLayout) => {
                    setIsInteracting(false);
                    updateActiveLayout(nextLayout, true);
                  }}
                >
                  {layout.map((item) => {
                    const selected = selectedCardId === item.i;
                    const chartMeta = charts[item.i];

                    return (
                      <div
                        key={item.i}
                        className={[
                          "dashboard-grid-item h-full",
                          selected ? "dashboard-grid-item-selected" : "",
                        ]
                          .filter(Boolean)
                          .join(" ")}
                        onMouseDownCapture={() => {
                          setSelectedCardId(item.i);
                        }}
                      >
                        <ChartCard
                          option={defaultChartOption}
                          title={chartMeta?.name || "默认名"}
                          selected={selected}
                          onSetting={() => {
                            console.log("设置图表", item.i, chartMeta);
                          }}
                          onExport={() => {
                            console.log("导出图片", item.i);
                          }}
                          onDelete={() => {
                            const nextLayout = layout.filter(
                              (layoutItem) => layoutItem.i !== item.i
                            );

                            setDashboards((prev) => {
                              return prev.map((dashboard) => {
                                if (dashboard.id !== activeDashboardId) {
                                  return dashboard;
                                }

                                const nextCharts = { ...dashboard.charts };
                                delete nextCharts[item.i];

                                return {
                                  ...dashboard,
                                  layout: nextLayout,
                                  charts: nextCharts,
                                };
                              });
                            });

                            persistDashboardLayout(nextLayout);
                            setSelectedCardId(null);
                          }}
                        />
                      </div>
                    );
                  })}
                </GridLayout>
              )}
            </div>
          </div>
        </main>
      </div>

      <CreateChartModal
        open={createChartOpen}
        onCancel={() => setCreateChartOpen(false)}
        onCreate={handleCreateChart}
      />
    </>
  );
};

export default Dashboard;