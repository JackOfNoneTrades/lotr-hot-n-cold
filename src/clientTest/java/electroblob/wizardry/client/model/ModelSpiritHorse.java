package electroblob.wizardry.client.model;

import net.minecraft.client.model.ModelHorse;

/**
 * Development-only replacement for the malformed class bundled inside War of the Ring 1.3.1.
 *
 * <p>
 * RetroFuturaGradle correctly remaps an SRG field in the bundled class, but the class already contains another
 * field with the resulting name. The duplicate makes the JVM reject the class before the client can start. A vanilla
 * horse model is compatible with Wizardry's spirit horse entity and keeps this workaround out of the published mod.
 */
public final class ModelSpiritHorse extends ModelHorse {
}
