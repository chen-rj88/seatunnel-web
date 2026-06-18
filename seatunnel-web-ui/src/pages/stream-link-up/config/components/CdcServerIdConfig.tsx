import { Alert, Col, Form, Input, Radio, Row } from 'antd';
import React from 'react';
import { validateServerIdRange } from '../serverId';

interface Props {
  className?: string;
}

const CdcServerIdConfig: React.FC<Props> = ({ className }) => {
  return (
    <div
      className={
        className ||
        'mb-5 rounded-2xl border border-slate-100 bg-slate-50/60 p-4'
      }
    >
      <div className="mb-3 text-sm font-semibold text-slate-700">
        MySQL-CDC Server ID
      </div>
      <Alert
        type="info"
        showIcon
        className="mb-4"
        message="为避免多个 MySQL-CDC 任务随机 server-id 冲突，建议为任务显式指定唯一的 server-id 或连续区间。"
      />

      <Row gutter={24}>
        <Col span={8}>
          <Form.Item label="分配方式" name="serverIdMode">
            <Radio.Group>
              <Radio value="MANUAL">手动指定</Radio>
              <Radio value="AUTO" disabled>
                平台自动分配
              </Radio>
            </Radio.Group>
          </Form.Item>
        </Col>

        <Col span={16}>
          <Form.Item
            noStyle
            shouldUpdate={(prev, current) =>
              prev.serverIdMode !== current.serverIdMode
            }
          >
            {({ getFieldValue }) => {
              const manualMode = getFieldValue('serverIdMode') !== 'AUTO';

              return (
                <Form.Item
                  label="server-id"
                  name="serverId"
                  extra="支持单个 ID 或连续区间，例如 5400 或 5400-5408；同一个 MySQL 集群内必须唯一。"
                  rules={[
                    {
                      validator: async (_, value) => {
                        if (!manualMode || !value) {
                          return Promise.resolve();
                        }

                        const result = validateServerIdRange(value);
                        if (!result.valid) {
                          return Promise.reject(new Error(result.message));
                        }

                        return Promise.resolve();
                      },
                    },
                  ]}
                >
                  <Input
                    disabled={!manualMode}
                    placeholder="例如：5400 或 5400-5408"
                    allowClear
                  />
                </Form.Item>
              );
            }}
          </Form.Item>
        </Col>
      </Row>
    </div>
  );
};

export default CdcServerIdConfig;
