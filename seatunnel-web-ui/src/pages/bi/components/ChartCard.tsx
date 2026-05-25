import React, { memo } from "react";

import AutoResizeChart from "./AutoResizeChart";

interface ChartCardProps {
  option: any;
  title?: string;
  selected?: boolean;
}

const ChartCard: React.FC<ChartCardProps> = memo(
  ({ option, title = "默认名", selected = false }) => {
    return (
      <div
        className={[
          "dashboard-card group relative z-10 box-border h-full overflow-hidden rounded-xl bg-white",
          "border shadow-sm transition-[border-color,box-shadow,transform] duration-200",
          "will-change-[transform,box-shadow,border-color]",
          selected
            ? [
                "border-[#3f51b5]",
                "shadow-[0_0_0_1px_#3f51b5,0_10px_30px_rgb(63_81_181_/_0.14),0_0_0_4px_rgb(63_81_181_/_0.08)]",
              ].join(" ")
            : [
                "border-slate-200",
                "hover:border-[#3f51b5]/50",
                "hover:shadow-[0_0_0_1px_rgb(63_81_181_/_0.14),0_10px_28px_rgb(63_81_181_/_0.10)]",
              ].join(" "),
        ]
          .filter(Boolean)
          .join(" ")}
      >
        <div
          className={[
            "drag-handle absolute left-1/2 top-2 z-20 grid w-4 -translate-x-1/2 cursor-move",
            "grid-cols-3 gap-0.5 opacity-70 transition duration-200",
            "group-hover:opacity-100",
          ].join(" ")}
          aria-hidden="true"
        >
          {Array.from({ length: 6 }).map((_, index) => (
            <span
              key={index}
              className="h-[3px] w-[3px] rounded-full bg-slate-400"
            />
          ))}
        </div>

        <div className="box-border flex h-9 items-center justify-between px-3 text-[13px] font-semibold text-slate-900">
          <span className="overflow-hidden text-ellipsis whitespace-nowrap">
            {title}
          </span>

          <button
            type="button"
            className={[
              "dashboard-card-more flex h-7 w-7 shrink-0 cursor-pointer items-center justify-center",
              "rounded-md border-none bg-transparent text-xl leading-none text-slate-500",
              "transition duration-200 hover:bg-slate-100 hover:text-slate-900",
            ].join(" ")}
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

        <div className="box-border h-[calc(100%-36px)] p-2">
          <AutoResizeChart option={option} />
        </div>
      </div>
    );
  },
);

ChartCard.displayName = "ChartCard";

export default ChartCard;