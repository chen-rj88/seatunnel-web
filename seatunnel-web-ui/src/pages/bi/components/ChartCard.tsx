import React, { memo } from "react";

import AutoResizeChart from "./AutoResizeChart";

interface ChartCardProps {
  option: any;
  title?: string;
}

const ChartCard: React.FC<ChartCardProps> = memo(
  ({ option, title = "默认名" }) => {
    return (
      <div className="dashboard-card">
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

export default ChartCard;