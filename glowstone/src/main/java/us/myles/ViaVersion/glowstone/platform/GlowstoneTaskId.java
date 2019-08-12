package us.myles.ViaVersion.glowstone.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import us.myles.ViaVersion.api.platform.TaskId;

@Getter
@AllArgsConstructor
public class GlowstoneTaskId implements TaskId {
    private Integer object;
}
