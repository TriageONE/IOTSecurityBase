package util;

import javax.usb.UsbDevice;

public class DeviceType {
    public enum DeviceTypes {
        ENV_SENSOR,
        CAMERA,
        INTERCOM,
        ALARM,
        OTHER,
        INVALID,
    }

    UsbDevice device;
    DeviceTypes type;
    public DeviceType(UsbDevice device) {
        this.device = device;
        this.type = findType(device);
    }

    private DeviceTypes findType(UsbDevice device) {
        short product = device.getUsbDeviceDescriptor().idProduct();
        short vendor = device.getUsbDeviceDescriptor().idVendor();
        DeviceTypes type;
        if (vendor == 0x0005){
            switch (product){
                case 1 -> type = DeviceTypes.ENV_SENSOR;
                case 4 -> type = DeviceTypes.ALARM;
                default -> type = DeviceTypes.OTHER;
            }
            return type;
        }
        if (vendor == 0x32e4){
            type = DeviceTypes.CAMERA;
            return type;
        } else
        return DeviceTypes.INVALID;
    }

    public DeviceTypes getType() { return this.type; }
    public UsbDevice getDevice() { return this.device; }

}
