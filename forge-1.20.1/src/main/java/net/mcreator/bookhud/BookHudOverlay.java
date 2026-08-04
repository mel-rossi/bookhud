/**
 * Forge (1.20.1) Docs : 
 * https://mcstreetguy.github.io/ForgeJavaDocs/1.20.1-47.1.0/
 * http://docs.minecraftforge.net/en/1.20.1/gui/screens/
*/
package net.mcreator.bookhud;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

// Event Registration 
@Mod.EventBusSubscriber(modid = BookhudMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BookHudOverlay {

	// Track active page locally on client 
	private static int clientCurrentPage = 1; 

	// Register memory slot for custom keybind entries 
	public static KeyMapping nextPageKey; 
	public static KeyMapping prevPageKey; 

	// Registering Keybinds (NeoForge & Forge) 
	@SubscribeEvent
	public static void registerKeyBindings(RegisterKeyMappingsEvent event) { 
		// Set Default Next Page - Key Bind : '['
		nextPageKey = new KeyMapping("Next Page", GLFW.GLFW_KEY_RIGHT_BRACKET, "BookHUD"); 
		// Set Default Prev Page - Key Bind : ']'
		prevPageKey = new KeyMapping("Prev Page", GLFW.GLFW_KEY_LEFT_BRACKET, "BookHUD");
		
		// Push assignments directly to Minecraft's global key registry
		event.register(nextPageKey); 
		event.register(prevPageKey);
	}

	// Forge Overlay Rendering 
	@SubscribeEvent 
	public static void registerOverlays (RegisterGuiOverlaysEvent event) {
		event.registerAbove(
			VanillaGuiOverlay.HOTBAR.id(),
			"overlay", 
			BookHudOverlay::render
		);
	}

	// Render Overlay Content - Forge 
	private static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) { 
		Minecraft mc = Minecraft.getInstance(); 
		if (mc.player == null || mc.level == null) return; 

		if (mc.screen != null) return; 

		ItemStack offhandItem = mc.player.getOffhandItem(); 
		if (!offhandItem.is(Items.WRITTEN_BOOK)) return; 

		// Boundary Handling 
		CompoundTag tag = offhandItem.getTag(); 
		if (tag == null || !tag.contains("pages") || !tag.contains("title")) return; 

		ListTag pagesTag = tag.getList("pages", Tag.TAG_STRING); 
		if (pagesTag.isEmpty()) return; 
		
		if (clientCurrentPage < 1) clientCurrentPage = 1; // Prevent scrolling before page 1

		if (clientCurrentPage > pagesTag.size()) clientCurrentPage = pagesTag.size(); // Prevent scrolling past last page 

		// Grab text using client-clamped mapping variable 
		String rawPageJson = pagesTag.getString(clientCurrentPage - 1); 
		Component pageComponent = Component.Serializer.fromJson(rawPageJson); 
		String pageText = pageComponent != null ? pageComponent.getString() : ""; 

		// Book title 
		String bookTitle = tag.getString("title");

		int boxWidth = 140; 
		int boxHeight = 160; 

		int xPos = screenWidth - boxWidth - 10; 
		int yPos = 10; 

		// HUD Box 
		guiGraphics.fill(xPos - 5, yPos - 5, screenWidth - 10, yPos + boxHeight, 0xAA000000);

		// Page Number
		guiGraphics.drawString(mc.font, "📄 Page " + clientCurrentPage, xPos, yPos, 0xFFFF55, false);

		// Text Wrapping 
		int currentLineY = yPos + 15; 
		for (net.minecraft.util.FormattedCharSequence line : mc.font.split(Component.literal(pageText), boxWidth)) { 
			guiGraphics.drawString(mc.font, line, xPos, currentLineY, 0xFFFFFF, false);
			currentLineY += 10; 
		}

		// Book Title 
		String titleDisplay = "📕 " + bookTitle;
		int titleWidth = mc.font.width(titleDisplay);
		int titleX = xPos + (boxWidth - titleWidth) / 2; // Center it
		int titleY = yPos + boxHeight - 12; // Position near bottom of box
		guiGraphics.drawString(mc.font, titleDisplay, titleX, titleY, 0x55FF55, false);
	}

	// Keyboard Click Listener 
	@Mod.EventBusSubscriber(modid = BookhudMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
	public static class ClientGameEvents { 
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) { 
			if (event.phase != TickEvent.Phase.END) return; 

			Minecraft mc = Minecraft.getInstance(); 
			if (mc.player == null) return; 

			// Check for written book on offhand 
			ItemStack offhandItem = mc.player.getOffhandItem(); 
			if (!offhandItem.is(Items.WRITTEN_BOOK)) return; 

			CompoundTag tag = offhandItem.getTag(); 
			if (tag == null || !tag.contains("pages")) return; 
			int pageCount = tag.getList("pages", Tag.TAG_STRING).size(); 
			if (pageCount == 0) return; 

			// Increment page selection by 1 
			if (nextPageKey != null && nextPageKey.consumeClick()) { 
				if (clientCurrentPage < pageCount) { 
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