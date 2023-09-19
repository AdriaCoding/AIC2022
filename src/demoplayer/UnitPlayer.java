package demoplayer;

import aic2022.user.*;

public class UnitPlayer {

	public void run(UnitController uc) {
		/*Insert here the code that should be executed only at the beginning of the unit's lifespan*/

		/*enemy team*/
		Team opponent = uc.getOpponent();

		while (true){
			/*Insert here the code that should be executed every round*/

			/*Generate a random number from 0 to 7, both included*/
			int randomNumber = (int)(Math.random()*8);

			/*Get corresponding direction*/
			Direction dir = Direction.values()[randomNumber];

			/*move in direction dir if possible*/
			if (uc.canMove(dir)) uc.move(dir);

			/*If this unit is a base, try spawning a barbarian at direction dir*/
			if (uc.getType() == UnitType.BASE) {
				if (uc.canSpawn(UnitType.BARBARIAN, dir)) uc.spawn(UnitType.BARBARIAN, dir);
			}

			/*Else, go through all visible units and attack the first one you see*/
			else {
				UnitInfo[] visibleEnemies = uc.senseUnits(opponent);
				for (int i = 0; i < visibleEnemies.length; ++i) {
					if (uc.canAttack(visibleEnemies[i].getLocation())) uc.attack(visibleEnemies[i].getLocation());
				}
			}

			uc.yield(); //End of turn
		}
	}
}

