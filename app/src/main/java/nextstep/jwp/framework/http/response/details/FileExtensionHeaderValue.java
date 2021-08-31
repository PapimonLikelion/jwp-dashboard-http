package nextstep.jwp.framework.http.response.details;

import java.util.Arrays;

public enum FileExtensionHeaderValue {

    HTML("text/html;charset=utf-8"),
    CSS("text/css"),
    JS("application/javascript"),
    SVG("image/svg+xml"),
    OTHER("");

    private final String headerValue;

    FileExtensionHeaderValue(final String headerValue) {
        this.headerValue = headerValue;
    }

    public static FileExtensionHeaderValue of(final String fileExtension) {
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(fileExtension))
                .findAny()
                .orElse(OTHER);
    }

    public String getHeaderValue() {
        return headerValue;
    }
}
