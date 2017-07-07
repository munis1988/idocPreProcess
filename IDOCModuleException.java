package com.saki.idocpreprocess;


import com.sap.aii.af.lib.mp.module.ModuleException;

public class IDOCModuleException extends ModuleException
{

    public IDOCModuleException(String msg)
    {
        super(msg);
    }

    public IDOCModuleException(String msg, Throwable e)
    {
        super(msg, e);
    }

    public IDOCModuleException(Throwable e)
    {
        super(e);
    }

    private static final long serialVersionUID = 0x73b92606e0148d28L;
}
