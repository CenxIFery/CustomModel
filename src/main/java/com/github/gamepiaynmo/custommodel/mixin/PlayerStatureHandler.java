package com.github.gamepiaynmo.custommodel.mixin;

import com.github.gamepiaynmo.custommodel.client.CustomModelClient;
import com.github.gamepiaynmo.custommodel.client.ModelPack;
import com.github.gamepiaynmo.custommodel.client.render.EntityDimensions;
import com.github.gamepiaynmo.custommodel.client.render.EntityPose;
import com.github.gamepiaynmo.custommodel.server.CustomModel;
import com.github.gamepiaynmo.custommodel.server.ModConfig;
import com.github.gamepiaynmo.custommodel.server.ModelInfo;
import com.google.common.collect.Maps;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

public class PlayerStatureHandler {
    public static EntityPose getPose(EntityLivingBase entity) {
        if (!entity.isEntityAlive()) return EntityPose.DYING;
        if (entity.isElytraFlying()) return EntityPose.FALL_FLYING;
        if (entity.isPlayerSleeping()) return EntityPose.SLEEPING;
        if (entity.isSneaking()) return EntityPose.SNEAKING;
        return EntityPose.STANDING;
    }

    public static final Map<EntityPose, EntityDimensions> defaultDimensions = Maps.newEnumMap(EntityPose.class);

    public static void setSize(EntityLivingBase entity, EntityDimensions dimensions) {
        if (dimensions.width != entity.width || dimensions.height != entity.height) {
            entity.width = dimensions.width;
            entity.height = dimensions.height;

            double d0 = (double)dimensions.width / 2.0D;
            entity.setEntityBoundingBox(new AxisAlignedBB(entity.posX - d0, entity.posY, entity.posZ - d0, entity.posX + d0, entity.posY + entity.height, entity.posZ + d0));
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        EntityPose pose = getPose(player);

        if (event.side == Side.CLIENT) {
            AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
            ModelPack pack = CustomModelClient.manager.getModelForPlayer(clientPlayer);
            if (pack != null) {
                //changed event phase to END, because START feels "delayed by 1 tick"
                //possibly because entity state is updated at phase "MIDDLE"?
                //also in vanilla, when you are not standing ON THE GROUND, sneaking will not change eye height,
                //but sneaking do change the "pose"
                if (CustomModelClient.serverConfig.customEyeHeight && event.phase == TickEvent.Phase.END) {
                    Float eyeHeight = pack.getModel().getModelInfo().eyeHeightMap.get(pose);
                    if (pose == EntityPose.SNEAKING && !player.onGround) {
                        eyeHeight = pack.getModel().getModelInfo().eyeHeightMap.get(EntityPose.STANDING);
                    }
                    if (eyeHeight != null)
                        player.eyeHeight = eyeHeight;
                    else player.eyeHeight = player.getDefaultEyeHeight();
                }

                if (CustomModelClient.serverConfig.customBoundingBox && event.phase == TickEvent.Phase.END) {
                    EntityDimensions dimensions = pack.getModel().getModelInfo().dimensionsMap.get(pose);
                    if (dimensions != null)
                        setSize(player, dimensions);
                }
            }
        } else {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            ModelInfo pack = CustomModel.manager.getModelForPlayer(serverPlayer);
            if (pack != null) {
                //changed event phase to END, because START feels "delayed by 1 tick"
                //possibly because entity state is updated at phase "MIDDLE"?
                //also in vanilla, when you are not standing ON THE GROUND, sneaking will not change eye height,
                //but sneaking do change the "pose"
                if (ModConfig.isCustomEyeHeight() && event.phase == TickEvent.Phase.END) {
                    Float eyeHeight = pack.eyeHeightMap.get(pose);
                    if (pose == EntityPose.SNEAKING && !player.onGround) {
                        eyeHeight = pack.eyeHeightMap.get(EntityPose.STANDING);
                    }
                    if (eyeHeight != null)
                        player.eyeHeight = eyeHeight;
                    else player.eyeHeight = player.getDefaultEyeHeight();
                }

                if (ModConfig.isCustomBoundingBox() && event.phase == TickEvent.Phase.END) {
                    EntityDimensions dimensions = pack.dimensionsMap.get(pose);
                    if (dimensions != null)
                        setSize(player, dimensions);
                }
            }
        }
    }

    static {
        defaultDimensions.put(EntityPose.DYING, new EntityDimensions(0.2f, 0.2f));
        defaultDimensions.put(EntityPose.FALL_FLYING, new EntityDimensions(0.6f, 0.6f));
        defaultDimensions.put(EntityPose.SLEEPING, new EntityDimensions(0.2f, 0.2f));
        defaultDimensions.put(EntityPose.SNEAKING, new EntityDimensions(0.6f, 1.65f));
        defaultDimensions.put(EntityPose.STANDING, new EntityDimensions(0.6f, 1.8f));
    }
}
