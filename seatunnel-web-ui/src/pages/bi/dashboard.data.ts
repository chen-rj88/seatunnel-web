import type { Layout } from "react-grid-layout";

export const COLS = 12;
export const ROW_HEIGHT = 45;
export const MARGIN: [number, number] = [8, 8];

export const MIN_ROW_COUNT = 32;
export const STORAGE_KEY = "dashboard-layout-v2";

export const DEFAULT_LAYOUT: Layout[] = [
  { i: "chart-1", x: 0, y: 0, w: 6, h: 10 },
  { i: "chart-2", x: 6, y: 0, w: 6, h: 10 },
  { i: "chart-3", x: 0, y: 10, w: 9, h: 8 },
];

export const chartTitleMap: Record<string, string> = {
  "chart-1": "参数分布",
  "chart-2": "执行统计",
  "chart-3": "任务趋势",
};

export const defaultChartOption = {
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
};