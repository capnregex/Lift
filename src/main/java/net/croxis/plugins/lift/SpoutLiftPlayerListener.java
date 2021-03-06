/*
 * This file is part of Lift.
 *
 * Copyright (c) ${project.inceptionYear}-2013, croxis <https://github.com/croxis/>
 *
 * Lift is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Lift is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Lift. If not, see <http://www.gnu.org/licenses/>.
 */
package net.croxis.plugins.lift;

import org.spout.api.entity.Player;
import org.spout.api.event.EventHandler;
import org.spout.api.event.Listener;
import org.spout.api.event.cause.PlayerCause;
import org.spout.api.event.player.Action;
import org.spout.api.event.player.PlayerInteractBlockEvent;
import org.spout.api.event.player.PlayerLeaveEvent;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.material.block.BlockFace;
import org.spout.vanilla.event.entity.EntityDamageEvent;
import org.spout.vanilla.component.block.material.Sign;
import org.spout.vanilla.material.VanillaMaterials;

public class SpoutLiftPlayerListener implements Listener{
	private SpoutLift plugin;

	public SpoutLiftPlayerListener(SpoutLift plugin){
		plugin.getEngine().getEventManager().registerEvents(this, plugin);
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractBlockEvent event){
		if (event.getAction() == Action.RIGHT_CLICK && event.getEntity().getWorld().getBlock(event.getPoint()) != null) {
			//TODO: There has got to be an easier way to do this
			Block signBlock = event.getEntity().getWorld().getBlock(event.getPoint());
			Block buttonBlock = signBlock.translate(BlockFace.BOTTOM);

			if (signBlock.getMaterial() == VanillaMaterials.WALL_SIGN
                && buttonBlock != null
			    && (buttonBlock.getMaterial() == VanillaMaterials.STONE_BUTTON || buttonBlock.getMaterial() == VanillaMaterials.WOOD_BUTTON)) {
				
				plugin.logDebug("Updating sign");
				Sign sign = signBlock.get(Sign.class);
				SpoutElevator elevator = SpoutElevatorManager.createLift(buttonBlock);
				
				if (elevator == null){
					plugin.logInfo("Elevator generation returned a null object. Please report circumstances that generated this error.");
					return;
				}
				
				if (elevator.getTotalFloors() < 1){
					// This is just a button and sign, not an elevator.
					return;
				} else if (elevator.getTotalFloors() == 1){
					((Player) event.getEntity()).sendMessage(SpoutLift.stringOneFloor);
					return;
				}
				
				event.setCancelled(true);
				
				int currentDestinationInt = 1;
				SpoutFloor currentFloor = elevator.getFloorFromY(buttonBlock.getY());
				String[] newText = new String[4];
				
				newText[0] = SpoutLift.stringCurrentFloor;
				newText[1] = Integer.toString(currentFloor.getFloor());
				newText[2] = "";
				newText[3] = "";
				try{
					String[] splits = sign.getText()[2].split(": ");
					currentDestinationInt = Integer.parseInt(splits[1]);	
				} catch (Exception e){
					currentDestinationInt = 0;
					plugin.logDebug("non Valid previous destination");
				}
				currentDestinationInt++;
				if (currentDestinationInt == currentFloor.getFloor()){
					currentDestinationInt++;
					plugin.logDebug("Skipping current floor");
				}
				// The following line MAY be what causes a potential bug for max floors
				if (currentDestinationInt > elevator.getTotalFloors()){
					currentDestinationInt = 1;
					if (currentFloor.getFloor() == 1)
						currentDestinationInt = 2;
					plugin.logDebug("Rotating back to first floor");
				}
				newText[2] = SpoutLift.stringDestination + " " + Integer.toString(currentDestinationInt);
				newText[3] = elevator.getFloorFromN(currentDestinationInt).getName();
				plugin.logDebug("Updating sign: " + newText.toString());
				sign.setText(newText, new PlayerCause((Player) event.getEntity()));
				//sign.
				plugin.logDebug("Completed sign update");
			}
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event){
		//disabled for now until vanilla is fixed
		/*if(event.getCause() == HealthChangeCause.DAMAGE && event.getDamageType() == DamageType.FALL){
			Entity faller = event.getEntity();
			if(SpoutElevatorManager.fallers.contains(faller)){
				event.setCancelled(true);
				SpoutElevatorManager.fallers.remove(faller);
			}
		}*/
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerLeaveEvent event){
		SpoutElevatorManager.removePlayer(event.getPlayer());
	}
}
