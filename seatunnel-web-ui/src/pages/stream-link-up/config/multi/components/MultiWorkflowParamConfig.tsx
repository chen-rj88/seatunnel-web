import {
  Alert,
  Col,
  Form,
  Input,
  InputNumber,
  Radio,
  Row,
  Select,
  Switch,
} from 'antd';
import React from 'react';
import {
  DATA_SAVE_MODE_OPTIONS,
  FIELD_IDE_OPTIONS,
  SCHEMA_SAVE_MODE_OPTIONS,
  validateServerIdRange,
} from '../config';

const MultiWorkflowParamConfig: React.FC = () => {
  return (
    <div className="mt-6 rounded-2xl bg-white" style={{ marginBottom: 40 }}>
      <div className="mb-5 text-base font-semibold text-slate-800">
        参数设置
      </div>

      <div className="mb-5 rounded-2xl border border-slate-100 bg-slate-50/60 p-4">
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

      <Row gutter={24}>
        <Col span={12}>
          <Form.Item
            label="每次拉取行数（Fetch Size）"
            name="fetchSize"
            rules={[{ required: true, message: '请输入每次拉取行数' }]}
          >
            <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
          </Form.Item>

          <Form.Item
            label="读取分片大小（Split Size）"
            name="splitSize"
            rules={[{ required: true, message: '请输入读取分片大小' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder="8096" />
          </Form.Item>
        </Col>

        <Col span={12}>
          <Row gutter={[16, 4]}>
            <Col span={12}>
              <Form.Item
                label="Schema 保存模式"
                name="schemaSaveMode"
                rules={[{ required: true, message: '请选择 Schema 保存模式' }]}
              >
                <Select
                  placeholder="请选择"
                  options={SCHEMA_SAVE_MODE_OPTIONS}
                />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                label="数据保存模式"
                name="dataSaveMode"
                rules={[{ required: true, message: '请选择数据保存模式' }]}
              >
                <Select placeholder="请选择" options={DATA_SAVE_MODE_OPTIONS} />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                label="启用 Upsert"
                name="enableUpsert"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={[16, 4]}>
            <Col span={12}>
              <Form.Item
                label="批次大小"
                name="batchSize"
                rules={[{ required: true, message: '请输入批次大小' }]}
              >
                <InputNumber
                  min={1}
                  placeholder="默认 10000"
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item label="字段标识格式" name="fieldIde">
                <Select placeholder="请选择" options={FIELD_IDE_OPTIONS} />
              </Form.Item>
            </Col>
          </Row>
        </Col>
      </Row>
    </div>
  );
};

export default MultiWorkflowParamConfig;
