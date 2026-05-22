import { Modal, Splitter } from "antd";
import React, { forwardRef, useImperativeHandle, useRef, useState } from "react";
import { useIntl } from "@umijs/max";




interface CreateModalProps {
  onCreate?: (values: any) => void;
}

const RealtimeTaskViewModal = forwardRef(({}: CreateModalProps, ref) => {


  const [visible, setVisible] = useState<boolean>(false);

  const callback = useRef<() => void>(() => {
    return;
  });

  const onClose = () => {
    setVisible(false);
   
  };

  const onOpen = (status: boolean, record: any, cbk: () => void) => {
    setVisible(status);
    callback.current = cbk;
  };

  useImperativeHandle(ref, () => ({
    onOpen,
  }));

  return (
    <Modal
      title={<></>}
      open={visible}
      onCancel={onClose}
      maskStyle={{ background: "#f2f4f7f2" }}
      destroyOnClose
      className="custom-modal"
      maskClosable={false}
      style={{ top: 10 }}
      width="99vw"
      footer={null}
    >
      <div style={{ height: "calc(100vh - 43px)", padding: 16 }}>
        

        
      </div>
    </Modal>
  );
});

export default RealtimeTaskViewModal;