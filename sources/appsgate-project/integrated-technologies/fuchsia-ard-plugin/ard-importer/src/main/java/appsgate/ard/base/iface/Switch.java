package appsgate.ard.base.iface;

import appsgate.ard.base.callback.LockerAuthorizationCallback;

public interface Switch extends Door {

    public void setHandshakeHandler(LockerAuthorizationCallback callback,String doorcode);

}
