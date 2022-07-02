package Murdak;

import aic2022.user.*;

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

			uc.yield(); //End of turn
		}
	}
}

