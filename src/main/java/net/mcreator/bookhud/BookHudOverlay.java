/*
 * NeoForge (1.21.1) Docs : https://docs.neoforged.net/docs/1.21.1/gui/screens
*/
package net.mcreator.bookhud;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

// Event Registration 
@EventBusSubscriber(modid = BookhudMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BookHudOverlay implements LayeredDraw.Layer {

	// Track active page locally on client
	private static int clientCurrentPage = 1;

	// Register memory slot for custom keybind entries
	public static KeyMapping nextPageKey; 
	public static KeyMapping prevPageKey; 

	// Registering Keybinds (NeoForge) 
	@SubscribeEvent 
	public static void RegisterKeyBindings(RegisterKeyMappingsEvent event) { 
		// Set Default Next Page Key Bind : '['
		nextPageKey = new KeyMapping("key.bookhud.next", GLFW.GLFW_KEY_RIGHT_BRACKET, "key.categories.misc");
		// Set Default Prev Page Key Bind : ']'
		prevPageKey = new KeyMapping("key.bookhud.prev", GLFW.GLFW_KEY_LEFT_BRACKET, "key.categories.misc");

		// Push assignments directly to Minecraft's global key registry
		event.register(nextPageKey); 
		event.register(prevPageKey);
	}

	@SubscribeEvent 
	public static void registerGuiLayers(RegisterGuiLayersEvent event) { 
		event.registerAbove(
			ResourceLocation.fromNamespaceAndPath("minecraft", "hotbar"), 
			ResourceLocation.fromNamespaceAndPath(BookhudMod.MODID, "overlay"),
			new BookHudOverlay()
		);
	}

	@Override 
	public void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) { 
		Minecraft mc = Minecraft.getInstance(); 
		if (mc.player == null || mc.level == null) return; 

		if (mc.screen != null) return;

		ItemStack offhandItem = mc.player.getOffhandItem(); 
		if (!offhandItem.is(Items.WRITTEN_BOOK)) return;

		// Boundary Handling  
		WrittenBookContent bookData = offhandItem.get(DataComponents.WRITTEN_BOOK_CONTENT); 
		if (bookData == null) return; 

		if (clientCurrentPage < 1) clientCurrentPage = 1; // Prevent scrolling before page 1

		if (clientCurrentPage > bookData.pages().size()) clientCurrentPage = bookData.pages().size(); // Prevent scorlling past last page 

		// Grab text using client-clamped mapping variable 
		String pageText = bookData.pages().get(clientCurrentPage - 1).raw().getString(); 

		int screenWidth = mc.getWindow().getGuiScaledWidth(); 
		int boxWidth = 140; 
		int boxHeight = 160; 

		int xPos = screenWidth - boxWidth - 10; 
		int yPos = 10; 

		// HUD Box 
		guiGraphics.fill(xPos - 5, yPos - 5, screenWidth - 10, yPos + boxHeight, 0xAA000000);

		// Text Wrapping 
		guiGraphics.drawString(mc.font, "📄 Page " + clientCurrentPage, xPos, yPos, 0xFFFF55, false);

		int currentLineY = yPos + 15; 
		for (net.minecraft.util.FormattedCharSequence line : mc.font.split(Component.literal(pageText), boxWidth)) { 
			guiGraphics.drawString(mc.font, line, xPos, currentLineY, 0xFFFFFF, false);
			currentLineY += 10; 
		}
	}

	// Keyboard Click Listener 
	@EventBusSubscriber(modid = BookhudMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT) 
	public static class ClientGameEvents { 
		@SubscribeEvent 
		public static void onClientTick(ClientTickEvent.Post event) { 
			Minecraft mc = Minecraft.getInstance(); 
			if (mc.player == null) return; 

			// Check for written book on offhand
			ItemStack offhandItem = mc.player.getOffhandItem();
			if (!offhandItem.is(Items.WRITTEN_BOOK)) return;
			WrittenBookContent bookData = offhandItem.get(DataComponents.WRITTEN_BOOK_CONTENT);
			if (bookData == null) return;

			// Increment page selection by 1 
			if (nextPageKey != null && nextPageKey.consumeClick()) { 
				if (clientCurrentPage < bookData.pages().size()) { 
					clientCurrentPage++; 
				}
			}
			
			// Decrement page selection by 1
			if (prevPageKey != null && prevPageKey.consumeClick()) {
				if (clientCurrentPage > 1) {
					clientCurrentPage--;
				}
			}
		}
	}
}
