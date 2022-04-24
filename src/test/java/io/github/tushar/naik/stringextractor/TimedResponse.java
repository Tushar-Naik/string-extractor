package io.github.tushar.naik.stringextractor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimedResponse<T> {
    public long time;
    public T response;
}
