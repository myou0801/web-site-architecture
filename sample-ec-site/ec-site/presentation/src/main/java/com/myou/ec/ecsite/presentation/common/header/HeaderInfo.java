package com.myou.ec.ecsite.presentation.common.header;

import java.time.LocalDateTime;

public record HeaderInfo(
    String loginId,
    String displayName,
    LocalDateTime previousLoginAt
) {
}
