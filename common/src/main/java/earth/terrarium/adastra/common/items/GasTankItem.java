package earth.terrarium.adastra.common.items;

import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.botarium.common.fluid.FluidApi;
import earth.terrarium.botarium.common.fluid.FluidConstants;
import earth.terrarium.botarium.common.fluid.base.BotariumFluidItem;
import earth.terrarium.botarium.common.fluid.base.FluidHolder;
import earth.terrarium.botarium.common.fluid.impl.SimpleFluidContainer;
import earth.terrarium.botarium.common.fluid.impl.WrappedItemFluidContainer;
import earth.terrarium.botarium.common.fluid.utils.ClientFluidHooks;
import earth.terrarium.botarium.common.item.ItemStackHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GasTankItem extends Item implements BotariumFluidItem<WrappedItemFluidContainer> {

    private final long tankSize;
    private final long distributionAmount;

    public GasTankItem(Properties properties, long tankSize, long distributionAmount) {
        super(properties);
        this.tankSize = tankSize;
        this.distributionAmount = distributionAmount;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (FluidUtils.hasFluid(stack)) {
            player.startUsingItem(usedHand);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        Inventory inventory = player.getInventory();
        var container = FluidUtils.getTank(stack);
        if (container.getFluidAmount() == 0) return;
        ItemStackHolder from = new ItemStackHolder(stack);
        if (!distributeSequential(from, container, inventory)) return;
        inventory.setItem(inventory.selected, from.getStack());
        if (entity.tickCount % 4 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, player.getSoundSource(), 1.0F, 1.0F);
        }
    }

    public boolean distributeSequential(ItemStackHolder from, FluidHolder container, Inventory inventory) {
        for (int i = inventory.getContainerSize() - 1; i >= 0; i--) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty() || stack.is(this)) continue;
            ItemStackHolder to = new ItemStackHolder(stack);
            long moved = FluidApi.moveFluid(from, to, container.copyWithAmount(FluidConstants.fromMillibuckets(distributionAmount)), false);
            inventory.setItem(i, to.getStack());
            if (moved > 0) return true;
        }
        return false;
    }

    @Override
    public WrappedItemFluidContainer getFluidContainer(ItemStack holder) {
        return new WrappedItemFluidContainer(
            holder,
            new SimpleFluidContainer(
                FluidConstants.fromMillibuckets(tankSize),
                1,
                (t, f) -> true));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(TooltipUtils.getFluidComponent(FluidUtils.getTank(stack), FluidUtils.getTankCapacity(stack)));
        tooltipComponents.add(TooltipUtils.getMaxFluidOutComponent(FluidConstants.fromMillibuckets(distributionAmount)));
        TooltipUtils.addDescriptionComponent(tooltipComponents, ConstantComponents.GAS_TANK_INFO);
    }

    public int getUseDuration(@NotNull ItemStack stack) {
        return 72_000;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return FluidUtils.hasFluid(stack);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var fluidContainer = getFluidContainer(stack);
        return (int) (((double) fluidContainer.getFirstFluid().getFluidAmount() / fluidContainer.getTankCapacity(0)) * 13);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return ClientFluidHooks.getFluidColor(FluidUtils.getTank(stack));
    }
}
