package org.microspring.test.primary;

import org.microspring.stereotype.Component;

@Component("inkjet")
public class InkjetPrinter implements Printer {
    @Override
    public String getPrinterType() {
        return "inkjet";
    }
} 