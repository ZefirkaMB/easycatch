package dev.easycatch.mixin;

import dev.easycatch.EasyCatchConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Shadow
    protected abstract void onHit(HitResult result);

    @Shadow
    public abstract Entity getOwner();

    /** true once the pearl has left the owner's (enlarged) hitbox, like vanilla's "leftOwner" */
    @Unique
    private boolean easycatch$leftOwner = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void easycatch$tick(CallbackInfo ci) {
        if (!EasyCatchConfig.enabled) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (self.isRemoved() || !(self.level() instanceof ServerLevel)) {
            return;
        }
        EntityType<?> type = self.getType();
        if (type == EntityType.ENDER_PEARL) {
            easycatch$pearl(self);
        } else if (type == EntityType.WIND_CHARGE) {
            easycatch$windCharge(self);
        }
    }

    @Unique
    private void easycatch$pearl(Entity self) {
        double scale = EasyCatchConfig.pearlHitboxScale;
        if (scale <= 1.0) {
            return;
        }
        Entity owner = this.getOwner();
        if (!(owner instanceof Player) || owner.isRemoved()) {
            return;
        }
        double grow = self.getBbWidth() * (scale - 1.0) / 2.0;
        AABB base = self.getBoundingBox().inflate(grow);
        AABB ownerBox = owner.getBoundingBox();

        if (!this.easycatch$leftOwner) {
            if (!base.intersects(ownerBox)) {
                this.easycatch$leftOwner = true;
            }
            return;
        }
        if (base.expandTowards(self.getDeltaMovement()).intersects(ownerBox)) {
            this.onHit(new EntityHitResult(owner));
        }
    }

    @Unique
    private void easycatch$windCharge(Entity self) {
        double scale = EasyCatchConfig.windChargeHitboxScale;
        if (scale <= 1.0) {
            return;
        }
        Entity owner = this.getOwner();
        double grow = self.getBbWidth() * (scale - 1.0) / 2.0;
        AABB box = self.getBoundingBox().inflate(grow).expandTowards(self.getDeltaMovement());

        List<Entity> targets = self.level().getEntities(self, box, e ->
                e != owner
                        && !e.isRemoved()
                        && !e.isSpectator()
                        && (e.getType() == EntityType.ENDER_PEARL || e instanceof LivingEntity));
        if (!targets.isEmpty()) {
            this.onHit(new EntityHitResult(targets.get(0)));
        }
    }
}
