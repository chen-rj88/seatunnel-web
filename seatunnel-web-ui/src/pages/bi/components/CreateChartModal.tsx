import { Checkbox, Input, Modal, Radio, Select } from "antd";
import {
  AreaChart,
  BarChart3,
  Hash,
  LineChart,
  PieChart,
  ScatterChart,
  Table2,
} from "lucide-react";
import React, { useMemo, useState } from "react";

export type ChartType =
  | "bar"
  | "line"
  | "areaLine"
  | "zline"
  | "combo"
  | "pie"
  | "ringPie"
  | "rosePie"
  | "scatter"
  | "table"
  | "funnel"
  | "wordCloud"
  | "number";

export interface CreateChartValues {
  name: string;
  chartType: ChartType;
  showLegend: boolean;
  showLabel: boolean;
  showAxis: boolean;
  showGrid: boolean;
  sortBy: "data" | "x" | "y";
  theme: string;
}

interface CreateChartModalProps {
  open: boolean;
  onCancel: () => void;
  onCreate: (values: CreateChartValues) => void;
}

const chartGroups: Array<{
  title: string;
  options: Array<{
    label: string;
    value: ChartType;
    icon: React.ReactNode;
  }>;
}> = [
  {
    title: "条形图",
    options: [
      {
        label: "柱状图",
        value: "bar",
        icon: <BarChart3 size={16} />,
      },
      {
        label: "条形图",
        value: "line",
        icon: <BarChart3 size={16} />,
      },
    ],
  },
  {
    title: "折线图",
    options: [
      {
        label: "折线图",
        value: "zline",
        icon: <LineChart size={16} />,
      },
      {
        label: "面积折线图",
        value: "areaLine",
        icon: <AreaChart size={16} />,
      },
    ],
  },
  {
    title: "组合图",
    options: [
      {
        label: "组合图",
        value: "combo",
        icon: <BarChart3 size={16} />,
      },
    ],
  },
  {
    title: "饼图",
    options: [
      {
        label: "饼图",
        value: "pie",
        icon: <PieChart size={16} />,
      },
      {
        label: "环形饼图",
        value: "ringPie",
        icon: <PieChart size={16} />,
      },
      {
        label: "玫瑰饼图",
        value: "rosePie",
        icon: <PieChart size={16} />,
      },
    ],
  },
  {
    title: "散点图",
    options: [
      {
        label: "散点图",
        value: "scatter",
        icon: <ScatterChart size={16} />,
      },
    ],
  },
  {
    title: "其他",
    options: [
      {
        label: "表格",
        value: "table",
        icon: <Table2 size={16} />,
      },
      {
        label: "漏斗图",
        value: "funnel",
        icon: <BarChart3 size={16} />,
      },
      {
        label: "词云",
        value: "wordCloud",
        icon: <Hash size={16} />,
      },
      {
        label: "统计数字",
        value: "number",
        icon: <Hash size={16} />,
      },
    ],
  },
];
const themeOptions = [
  {
    label: "蓝色系",
    value: "blue",
    colors: ["#1f8ee5", "#2499ed", "#3da8f3", "#59b4f4", "#7fc5f5", "#b7ddf6"],
  },
  {
    label: "紫灰色系",
    value: "purpleGray",
    colors: ["#d7b7ea", "#e8d9f3", "#f4eef9", "#c6a5c3", "#f6c7d9", "#a9c38f"],
  },
  {
    label: "淡紫色系",
    value: "purple",
    colors: ["#feb1d9", "#d695e1", "#ad8fdb", "#8477c6", "#86ddf4", "#72bcec"],
  },
  {
    label: "淡米色系",
    value: "beige",
    colors: ["#f9f3d3", "#fad6b5", "#fcafae", "#fedfe5", "#ddf0ee", "#b6e3e8"],
  },
  {
    label: "淡棕色系",
    value: "brown",
    colors: ["#efcfaf", "#efae6b", "#e4aa72", "#dd936f", "#b9744e", "#ba6830"],
  },
  {
    label: "黄色系",
    value: "yellow",
    colors: ["#ffa001", "#ffb400", "#ffc107", "#ffca27", "#ffd550", "#fee180"],
  },
  {
    label: "绿色系",
    value: "green",
    colors: ["#679f38", "#7db343", "#8bc349", "#9ace63", "#afd582", "#c5e1a5"],
  },
  {
    label: "橙色系",
    value: "orange",
    colors: ["#f67d01", "#fb8c00", "#ff9800", "#ffa725", "#feb74d", "#ffcc80"],
  },
];

const ThemeColorBar: React.FC<{
  colors: string[];
  compact?: boolean;
}> = ({ colors, compact = false }) => {
  return (
    <div
      className={[
        "flex overflow-hidden rounded-full",
        compact ? "h-3 w-[220px]" : "h-5 w-[232px]",
      ].join(" ")}
    >
      {colors.map((color) => (
        <span
          key={color}
          className="flex-1"
          style={{ backgroundColor: color }}
        />
      ))}
    </div>
  );
};

const CreateChartModal: React.FC<CreateChartModalProps> = ({
  open,
  onCancel,
  onCreate,
}) => {
  const [name, setName] = useState("默认名");
  const [chartType, setChartType] = useState<ChartType>("bar");

  const [showLegend, setShowLegend] = useState(false);
  const [showLabel, setShowLabel] = useState(false);
  const [showAxis, setShowAxis] = useState(true);
  const [showGrid, setShowGrid] = useState(true);
  const [themeOpen, setThemeOpen] = useState(false);

  const [chartTypeOpen, setChartTypeOpen] = useState(false);

  const [sortBy, setSortBy] = useState<"data" | "x" | "y">("data");
  const [theme, setTheme] = useState("blue");
  const selectedTheme = useMemo(() => {
    return themeOptions.find((item) => item.value === theme) || themeOptions[0];
  }, [theme]);
  const selectedChart = useMemo(() => {
    return chartGroups
      .flatMap((group) => group.options)
      .find((item) => item.value === chartType);
  }, [chartType]);

  const handleCreate = () => {
    onCreate({
      name: name?.trim() || "默认名",
      chartType,
      showLegend,
      showLabel,
      showAxis,
      showGrid,
      sortBy,
      theme,
    });
  };

  return (
    <Modal
      open={open}
      width="88vw"
      centered
      footer={null}
      closable
      destroyOnClose={false}
      onCancel={onCancel}
      className="create-chart-modal"
      styles={{
        body: {
          padding: 0,
        },
      }}
    >
      <div className="flex h-[88vh] min-h-[640px] overflow-hidden rounded-2xl bg-white">
        <section className="flex min-w-0 flex-1 flex-col p-6">
          <Input
            value={name}
            onChange={(event) => setName(event.target.value)}
            variant="borderless"
            className="!w-[280px] !px-0 !text-base !font-semibold"
            placeholder="请输入图表名称"
          />

          <div className="mt-4 flex flex-1 items-center justify-center rounded-2xl border border-slate-100 bg-white">
            <div className="w-[72%]">
              <div className="h-[1px] w-full bg-slate-400" />
            </div>
          </div>
        </section>

        <aside className="flex w-[360px] shrink-0 flex-col border-l border-slate-100 bg-white">
          <div className="flex h-12 shrink-0 items-center justify-center border-b border-slate-100">
            <div className="rounded-md bg-[hsl(231_48%_96%)] px-3 py-1 text-sm font-medium text-[hsl(231_48%_48%)]">
              数据配置
            </div>
          </div>

          <div className="min-h-0 flex-1 overflow-auto px-5 py-4">
            <div className="space-y-5">
              <div>
                <div className="mb-2 text-sm font-semibold text-slate-900">
                  图表类型
                </div>

                <Select
                  value={chartType}
                  open={chartTypeOpen}
                  onOpenChange={setChartTypeOpen}
                  className="w-full"
                  popupClassName="create-chart-type-dropdown"
                  onChange={(value) => {
                    setChartType(value);
                    setChartTypeOpen(false);
                  }}
                  options={chartGroups.flatMap((group) =>
                    group.options.map((item) => ({
                      label: (
                        <div className="flex items-center gap-2">
                          {item.icon}
                          <span>{item.label}</span>
                        </div>
                      ),
                      value: item.value,
                    }))
                  )}
                  popupRender={() => (
                    <div className="rounded-xl bg-white p-3">
                      {chartGroups.map((group) => (
                        <div key={group.title} className="mb-4 last:mb-0">
                          <div className="mb-2 text-sm font-semibold text-slate-800">
                            {group.title}
                          </div>

                          <div className="grid grid-cols-3 gap-2">
                            {group.options.map((item) => {
                              const active = item.value === chartType;

                              return (
                                <button
                                  key={`${group.title}-${item.value}-${item.label}`}
                                  type="button"
                                  onMouseDown={(event) => {
                                    event.preventDefault();
                                  }}
                                  onClick={() => {
                                    setChartType(item.value);
                                    setChartTypeOpen(false);
                                  }}
                                  className={[
                                    "flex h-14 flex-col items-center justify-center gap-1 rounded-lg border text-xs transition",
                                    active
                                      ? "border-[hsl(231_48%_48%)] bg-[hsl(231_48%_96%)] text-[hsl(231_48%_48%)] shadow-[0_0_0_1px_hsl(231_48%_48%_/_0.18)]"
                                      : "border-transparent bg-slate-50 text-slate-600 hover:border-slate-200 hover:bg-slate-100",
                                  ].join(" ")}
                                >
                                  {item.icon}
                                  <span>{item.label}</span>
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                />
              </div>

              <div>
                <div className="mb-2 text-sm font-semibold text-slate-900">
                  横轴
                </div>
                <Select className="w-full" placeholder="请选择横轴字段" />
              </div>

              <div>
                <div className="mb-2 text-sm font-semibold text-slate-900">
                  纵轴
                </div>
                <Select className="w-full" placeholder="请选择纵轴字段" />
              </div>

              <div>
                <div className="mb-3 text-sm font-semibold text-slate-900">
                  图表选项
                </div>

                <div className="grid grid-cols-2 gap-3 text-sm">
                  <Checkbox
                    checked={showLegend}
                    onChange={(event) => setShowLegend(event.target.checked)}
                  >
                    图例
                  </Checkbox>

                  <Checkbox
                    checked={showLabel}
                    onChange={(event) => setShowLabel(event.target.checked)}
                  >
                    数据标签
                  </Checkbox>

                  <Checkbox
                    checked={showAxis}
                    onChange={(event) => setShowAxis(event.target.checked)}
                  >
                    坐标轴
                  </Checkbox>

                  <Checkbox
                    checked={showGrid}
                    onChange={(event) => setShowGrid(event.target.checked)}
                  >
                    网格线
                  </Checkbox>
                </div>
              </div>

              <div>
                <div className="mb-3 text-sm font-semibold text-slate-900">
                  排序依据
                </div>

                <Radio.Group
                  value={sortBy}
                  onChange={(event) => setSortBy(event.target.value)}
                >
                  <Radio value="data">数据顺序</Radio>
                  <Radio value="x">横轴值</Radio>
                  <Radio value="y">纵轴值</Radio>
                </Radio.Group>
              </div>

              <div>
                <div className="mb-2 text-sm font-semibold text-slate-900">
                  主题颜色
                </div>

                <Select
                  value={theme}
                  open={themeOpen}
                  onOpenChange={setThemeOpen}
                  className="w-full create-theme-select"
                  popupClassName="create-theme-dropdown"
                  suffixIcon={<span className="text-slate-400">⌄</span>}
                  options={themeOptions.map((item) => ({
                    value: item.value,
                    label: <ThemeColorBar colors={item.colors} compact />,
                  }))}
                  onChange={(value) => {
                    setTheme(value);
                    setThemeOpen(false);
                  }}
                  labelRender={() => (
                    <ThemeColorBar colors={selectedTheme.colors} compact />
                  )}
                  popupRender={() => (
                    <div className="rounded-xl bg-white p-3">
                      <div className="max-h-[260px] space-y-3 overflow-y-auto pr-1">
                        {themeOptions.map((item) => {
                          const active = item.value === theme;

                          return (
                            <button
                              key={item.value}
                              type="button"
                              title={item.label}
                              onMouseDown={(event) => {
                                event.preventDefault();
                              }}
                              onClick={() => {
                                setTheme(item.value);
                                setThemeOpen(false);
                              }}
                              className={[
                                "flex w-full items-center rounded-lg px-3 py-2 transition",
                                active
                                  ? "bg-[hsl(231_48%_96%)]"
                                  : "hover:bg-slate-50",
                              ].join(" ")}
                            >
                              <ThemeColorBar colors={item.colors} />
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                />
              </div>

              <div className="rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-500">
                当前选择：
                <span className="font-medium text-slate-700">
                  {selectedChart?.label || "柱状图"}
                </span>
              </div>
            </div>
          </div>

          <div className="shrink-0 border-t border-slate-100 p-5">
            <button
              type="button"
              onClick={handleCreate}
              className="h-10 w-full rounded-lg bg-[hsl(231_48%_48%)] text-sm font-medium text-white transition hover:bg-[hsl(231_48%_42%)]"
            >
              保存
            </button>
          </div>
        </aside>
      </div>
    </Modal>
  );
};

export default CreateChartModal;
