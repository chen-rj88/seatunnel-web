import {
  CheckCircleFilled,
  CheckOutlined,
  CloseOutlined,
  CopyOutlined,
  MacCommandOutlined,
  PauseCircleOutlined,
  StopOutlined,
  SyncOutlined,
} from "@ant-design/icons";
import { message, Popover } from "antd";
import React, { useEffect, useRef, useState } from "react";

interface TaskStatusProps {
  status?: string;
  errorMessage?: string;
}

const statusConfig: Record<
  string,
  {
    color: string;
    icon: React.ReactNode;
    text: string;
  }
> = {
  FINISHED: {
    color: "#52c41a",
    icon: <CheckCircleFilled />,
    text: "FINISHED",
  },
  RUNNING: {
    color: "#1677ff",
    icon: <SyncOutlined spin />,
    text: "RUNNING",
  },
  FAILED: {
    color: "#ff4d4f",
    icon: <CloseOutlined />,
    text: "FAILED",
  },
  CANCELED: {
    color: "#8c8c8c",
    icon: <StopOutlined />,
    text: "CANCELED",
  },
  PAUSED: {
    color: "#faad14",
    icon: <PauseCircleOutlined />,
    text: "PAUSED",
  },
};

const TaskStatus: React.FC<TaskStatusProps> = ({ status, errorMessage }) => {
  const config = status ? statusConfig[status] : undefined;
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current) {
        window.clearTimeout(timerRef.current);
      }
    };
  }, []);

  const handleCopy = async (e?: React.MouseEvent) => {
    e?.stopPropagation();

    if (!errorMessage) return;

    try {
      await navigator.clipboard.writeText(errorMessage);
      setCopied(true);

      if (timerRef.current) {
        window.clearTimeout(timerRef.current);
      }

      timerRef.current = window.setTimeout(() => {
        setCopied(false);
      }, 1800);
    } catch (err) {
      message.error("复制失败，请手动复制");
    }
  };

  if (!config) {
    return (
      <span className="inline-flex items-center text-[#999]">
        <MacCommandOutlined className="mr-1.5" />
        <span className="font-medium">NOT STARTED</span>
      </span>
    );
  }

  const content = (
    <span
      className="inline-flex items-center font-medium"
      style={{ color: config.color }}
    >
      <span className="mr-1.5 inline-flex items-center">{config.icon}</span>
      <span>{config.text}</span>
    </span>
  );

  if (status === "FAILED" && errorMessage) {
    const lines = errorMessage.split("\n");

    return (
      <>
        <style>
          {`
            @keyframes copySuccessPop {
              0% {
                transform: scale(0.92);
                opacity: 0.75;
              }
              60% {
                transform: scale(1.06);
                opacity: 1;
              }
              100% {
                transform: scale(1);
                opacity: 1;
              }
            }
          `}
        </style>

        <Popover
          placement="right"
          trigger="hover"
          title={null}
          overlayInnerStyle={{
            padding: 0,
            borderRadius: 14,
            overflow: "hidden",
            boxShadow: "0 18px 45px rgba(15, 23, 42, 0.18)",
          }}
          content={
            <div className="w-[520px] overflow-hidden rounded-[14px] border border-white/10 bg-[#0f172a] font-mono text-[13px] leading-[1.6]">
              <div className="flex h-10 items-center justify-between border-b border-white/10 bg-white/[0.03] px-3">
                <div className="flex items-center gap-1.5">
                  <span className="h-2.5 w-2.5 rounded-full bg-[#ff5f57]" />
                  <span className="h-2.5 w-2.5 rounded-full bg-[#ffbd2e]" />
                  <span className="h-2.5 w-2.5 rounded-full bg-[#28c840]" />
                </div>

                <button
                  type="button"
                  onClick={handleCopy}
                  className={[
                    "inline-flex items-center justify-center gap-1.5 rounded-full border px-2.5 py-1 text-xs transition-all duration-200",
                    copied
                      ? "border-[#86efac]/80 bg-[#f0fdf4] text-[#16a34a]"
                      : "border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white",
                  ].join(" ")}
                  style={{
                    animation: copied ? "copySuccessPop 0.28s ease" : "none",
                  }}
                >
                  {copied ? <CheckOutlined /> : <CopyOutlined />}
                  <span>{copied ? "COPYED" : "COPY"}</span>
                </button>
              </div>

              <div className="max-h-[240px] min-h-[120px] overflow-auto px-3 py-3">
                {lines.map((line, index) => (
                  <div key={index} className="flex items-start">
                    <span className="w-9 shrink-0 select-none pr-3 text-right text-[#64748b]">
                      {index + 1}
                    </span>

                    <span className="flex-1 whitespace-pre-wrap break-words text-[rgb(0,255,136)]">
                      {line || " "}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          }
        >
          <span className="cursor-pointer">{content}</span>
        </Popover>
      </>
    );
  }

  return content;
};

export default TaskStatus;
