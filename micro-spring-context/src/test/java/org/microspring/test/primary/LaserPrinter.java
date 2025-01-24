package org.microspring.test.primary;

import org.microspring.context.annotation.Primary;
import org.microspring.stereotype.Component;

@Component
@Primary
public class LaserPrinter implements Printer {
    @Override
    public String getPrinterType() {
        return "laser";
    }
} 