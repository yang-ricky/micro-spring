package org.microspring.test.primary;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.beans.factory.annotation.Autowired;

@Component
public class PrinterUser {
    private final Printer defaultPrinter;
    private final Printer inkjetPrinter;

    @Autowired
    public PrinterUser(Printer defaultPrinter, @Qualifier("inkjet") Printer inkjetPrinter) {
        this.defaultPrinter = defaultPrinter;
        this.inkjetPrinter = inkjetPrinter;
    }

    public String getDefaultPrinterType() {
        return defaultPrinter.getPrinterType();
    }

    public String getInkjetPrinterType() {
        return inkjetPrinter.getPrinterType();
    }
} 