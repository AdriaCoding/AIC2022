package Murdak_v5;

import aic2022.user.UnitController;
import aic2022.user.UnitType;

public class UnitPlayer {

	public void run(UnitController uc) {

		while (true){
			if (uc.getType() == UnitType.BASE) {
				Base base = new Base(uc);
				base.run();
			}
			else if (uc.getType() == UnitType.RANGER) {
				Ranger ranger = new Ranger(uc);
				ranger.run();
			}
			else if (uc.getType() == UnitType.BARBARIAN	) {
				Barbarian barbarian = new Barbarian(uc);
				barbarian.run();
			}
			else if (uc.getType() == UnitType.EXPLORER	) {
				Explorer explorer = new Explorer(uc);
				explorer.run();
			}
			else if (uc.getType() == UnitType.KNIGHT	) {
				Knight knight = new Knight(uc);
				knight.run();
			}

			uc.yield(); //End of turn
		}
	}
}

