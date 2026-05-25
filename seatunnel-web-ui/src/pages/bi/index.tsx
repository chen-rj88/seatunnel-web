import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import GridLayout, { Layout } from "react-grid-layout";
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

import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";
import "./index.less";

import ChartCard from "./components/ChartCard";
import {
  COLS,
  MARGIN,
  ROW_HEIGHT,
  chartTitleMap,
  defaultChartOption,
} from "./dashboard.data";
import {
  getCanvasHeight,
  getInitialLayout,
  getLayoutBottomRow,
  persistDashboardLayout,
} from "./dashboard.utils";

interface DashboardItem {
  id: string;
  name: string;
  layout: Layout[];
}

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
    },
  ]);

  const [activeDashboardId, setActiveDashboardId] = useState("default");
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const [canvasWidth, setCanvasWidth] = useState(1200);
  const [isEditing, setIsEditing] = useState(false);
  const [isInteracting, setIsInteracting] = useState(false);

  const activeDashboard = useMemo(() => {
    return dashboards.find((item) => item.id === activeDashboardId);
  }, [dashboards, activeDashboardId]);

  const layout = activeDashboard?.layout || [];

  const gridWidth = canvasWidth;

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
    [activeDashboardId],
  );

  const handleCreateDashboard = () => {
    const nextDashboard: DashboardItem = {
      id: createDashboardId(),
      name: `新仪表盘 ${dashboards.length + 1}`,
      layout: [],
    };

    setDashboards((prev) => [...prev, nextDashboard]);
    setActiveDashboardId(nextDashboard.id);
    setSelectedCardId(null);
    setIsEditing(true);
  };

  const handleCreateChart = () => {
    const nextChartId = createChartId();

    const bottomRow = getLayoutBottomRow(layout);

    const nextLayout: Layout[] = [
      ...layout,
      {
        i: nextChartId,
        x: 0,
        y: bottomRow,
        w: 6,
        h: 5,
        minW: 3,
        minH: 3,
      },
    ];

    updateActiveLayout(nextLayout, true);
    setSelectedCardId(nextChartId);
    setIsEditing(true);
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
    // 这里后面可以接接口刷新图表数据
    console.log("refresh dashboard");
  };

  return (
    <div className="flex h-screen overflow-hidden bg-[#f7f7f8]">
      {/* 左侧仪表盘列表 */}
      <aside className="flex w-[224px] shrink-0 flex-col border-r border-slate-200 bg-white">
        <div className="flex h-14 items-center gap-2 border-b border-slate-100 px-4">
          <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-violet-50 text-violet-600">
            <LayoutDashboard size={17} />
          </div>

          <div className="min-w-0">
            <div className="text-sm font-semibold text-slate-900">仪表盘</div>
            <div className="text-xs text-slate-400">Dashboard</div>
          </div>
        </div>

        <div className="flex items-center justify-between px-3 pb-2 pt-4">
          <span className="text-xs font-medium text-slate-400">仪表盘列表</span>

          <button
            type="button"
            onClick={handleCreateDashboard}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition hover:bg-violet-50 hover:text-violet-600"
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
                    ? "bg-violet-50 font-medium text-violet-700"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                ].join(" ")}
              >
                <BarChart3 size={15} />
                <span className="min-w-0 flex-1 truncate">{dashboard.name}</span>
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

      {/* 右侧主区域 */}
      <main className="flex min-w-0 flex-1 flex-col">
        {/* 顶部工具栏 */}
        <header className="flex h-14 shrink-0 items-center justify-between border-b border-slate-200 bg-white px-4">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
              <LayoutDashboard size={18} />
            </div>

            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h1 className="truncate text-sm font-semibold text-slate-900">
                  {activeDashboard?.name || "未命名仪表盘"}
                </h1>

                {isEditing && (
                  <span className="rounded-full bg-violet-50 px-2 py-0.5 text-[11px] font-medium text-violet-600">
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
              onClick={handleCreateChart}
              className="flex h-9 items-center gap-1.5 rounded-xl border border-violet-200 bg-violet-50 px-3 text-sm font-medium text-violet-600 transition hover:border-violet-300 hover:bg-violet-100"
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
                  : "bg-violet-600 text-white hover:bg-violet-700",
              ].join(" ")}
            >
              {isEditing ? <Check size={15} /> : <Edit3 size={15} />}
              {isEditing ? "完成" : "编辑"}
            </button>
          </div>
        </header>

        {/* 画布 */}
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
                  <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-violet-50 text-violet-600">
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
                    onClick={handleCreateChart}
                    className="mt-5 flex h-9 items-center gap-1.5 rounded-xl bg-violet-600 px-4 text-sm font-medium text-white transition hover:bg-violet-700"
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
                width={gridWidth}
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
                        title={chartTitleMap[item.i] || "默认名"}
                        selected={selected}
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
  );
};

export default Dashboard;