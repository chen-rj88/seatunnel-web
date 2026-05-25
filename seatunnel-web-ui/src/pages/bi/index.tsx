import ReactECharts from "echarts-for-react";
import React, {
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import GridLayout, { Layout } from "react-grid-layout";

import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";
import "./index.less";

const COLS = 12;
const ROW_HEIGHT = 45;
const MARGIN: [number, number] = [8, 8];

const MIN_ROW_COUNT = 32;
const STORAGE_KEY = "dashboard-layout-v2";

const DEFAULT_LAYOUT: Layout[] = [
  { i: "chart-1", x: 0, y: 0, w: 6, h: 10 },
  { i: "chart-2", x: 6, y: 0, w: 6, h: 10 },
  { i: "chart-3", x: 0, y: 10, w: 9, h: 8 },
];

const getInitialLayout = (): Layout[] => {
  try {
    const storedLayout = localStorage.getItem(STORAGE_KEY);

    if (!storedLayout) {
      return DEFAULT_LAYOUT;
    }

    const parsedLayout = JSON.parse(storedLayout);

    if (Array.isArray(parsedLayout) && parsedLayout.length > 0) {
      return parsedLayout;
    }

    return DEFAULT_LAYOUT;
  } catch (error) {
    console.warn("Failed to parse dashboard layout:", error);
    return DEFAULT_LAYOUT;
  }
};

const getLayoutBottomRow = (layout: Layout[]) => {
  if (!layout.length) {
    return MIN_ROW_COUNT;
  }

  return Math.max(...layout.map((item) => item.y + item.h), MIN_ROW_COUNT);
};

const getCanvasHeight = (rowCount: number) => {
  const [, marginY] = MARGIN;
  return rowCount * ROW_HEIGHT + Math.max(rowCount - 1, 0) * marginY;
};

const getColWidth = (containerWidth: number) => {
  const [marginX] = MARGIN;

  if (containerWidth <= 0) {
    return 0;
  }

  return (containerWidth - marginX * (COLS - 1)) / COLS;
};

interface AutoResizeChartProps {
  option: any;
}

const AutoResizeChart: React.FC<AutoResizeChartProps> = memo(({ option }) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<any>(null);

  useEffect(() => {
    if (!wrapperRef.current) {
      return;
    }

    const resize = () => {
      chartRef.current?.getEchartsInstance()?.resize();
    };

    resize();

    const observer = new ResizeObserver(() => {
      resize();
    });

    observer.observe(wrapperRef.current);

    return () => {
      observer.disconnect();
    };
  }, []);

  return (
    <div ref={wrapperRef} className="dashboard-chart-wrapper">
      <ReactECharts
        ref={chartRef}
        option={option}
        notMerge
        lazyUpdate
        style={{
          width: "100%",
          height: "100%",
        }}
      />
    </div>
  );
});

AutoResizeChart.displayName = "AutoResizeChart";

interface ChartCardProps {
  option: any;
  title?: string;
}

const ChartCard: React.FC<ChartCardProps> = memo(
  ({ option, title = "默认名" }) => {
    return (
      <div className="dashboard-card">
        {/* 顶部正中间拖拽把手 */}
        <div className="dashboard-card-top-grip drag-handle" aria-hidden="true">
          <span />
          <span />
          <span />
          <span />
          <span />
          <span />
        </div>

        <div className="dashboard-card-header">
          <span className="dashboard-card-title">{title}</span>

          <button
            type="button"
            className="dashboard-card-more"
            onMouseDown={(event) => {
              event.stopPropagation();
            }}
            onClick={(event) => {
              event.stopPropagation();
            }}
          >
            ⋮
          </button>
        </div>

        <div className="dashboard-card-body">
          <AutoResizeChart option={option} />
        </div>
      </div>
    );
  }
);

ChartCard.displayName = "ChartCard";

const Dashboard = () => {
  const canvasInnerRef = useRef<HTMLDivElement>(null);
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);

  const [canvasWidth, setCanvasWidth] = useState(1200);
  const [layout, setLayout] = useState<Layout[]>(getInitialLayout);
  const [isEditing, setIsEditing] = useState(false);

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

  const chartOption = useMemo(
    () => ({
      color: ["hsl(231 48% 48%)"],
      animation: false,
      grid: {
        top: 28,
        right: 20,
        bottom: 72,
        left: 56,
      },
      xAxis: {
        type: "category",
        data: [
          "ANALYZE_AUTO",
          "COMPRESS",
          "CLOSE_DELAY",
          "TABLE_ENGINE",
          "EXCLUSIVE",
          "QUERY_TIMEOUT",
          "DISTINCT",
          "CACHE_SIZE",
          "REUSE_SPACE",
          "FILE_WRITE",
        ],
        axisLabel: {
          rotate: 90,
          color: "#6b7280",
          fontSize: 12,
        },
        axisLine: {
          lineStyle: {
            color: "#9ca3af",
          },
        },
        axisTick: {
          alignWithLabel: true,
        },
      },
      yAxis: {
        type: "value",
        axisLabel: {
          color: "#6b7280",
          fontSize: 12,
        },
        splitLine: {
          show: false,
        },
      },
      series: [
        {
          type: "bar",
          barWidth: 5,
          data: [
            12000, 32000, 180000, 50000, 230000, 300000, 80000, 260000, 16000,
            9000,
          ],
        },
      ],
    }),
    []
  );

  const persistLayout = useCallback((nextLayout: Layout[]) => {
    setLayout(nextLayout);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextLayout));
  }, []);

  useEffect(() => {
    console.log("selectedCardId changed:", selectedCardId);
  }, [selectedCardId]);

  return (
    <div className="dashboard-page">
      <div className="dashboard-canvas">
        <div
          ref={canvasInnerRef}
          className="dashboard-canvas-inner"
          style={{
            minHeight: canvasMinHeight,
          }}
        >
          <GridLayout
            className="dashboard-layout"
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
            isDraggable
            isResizable
            isBounded
            onDragStart={(_, oldItem) => {
              setIsEditing(true);
              setSelectedCardId(oldItem.i);
            }}
            onResizeStart={(_, oldItem) => {
              setIsEditing(true);
              setSelectedCardId(oldItem.i);
            }}
            onDrag={(nextLayout) => {
              setLayout(nextLayout);
            }}
            onResize={(nextLayout) => {
              setLayout(nextLayout);
            }}
            onDragStop={(nextLayout) => {
              setIsEditing(false);
              persistLayout(nextLayout);
            }}
            onResizeStop={(nextLayout) => {
              setIsEditing(false);
              persistLayout(nextLayout);
            }}
          >
            {layout.map((item) => {
              return (
                <div
                  key={item.i}
                  className={[
                    "dashboard-grid-item",
                    selectedCardId === item.i
                      ? "dashboard-grid-item-selected"
                      : "",
                  ]
                    .filter(Boolean)
                    .join(" ")}
                  onMouseDownCapture={() => {
                    setSelectedCardId(item.i);
                  }}
                >
                  <ChartCard option={chartOption} />
                </div>
              );
            })}
          </GridLayout>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
