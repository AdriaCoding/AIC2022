package Murdak_v3;

import aic2022.user.*;

public class Barbarian extends CombatUnit {

    public Barbarian (UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
        this.movement = new Movement(uc, data);
    }

    void run() {

        while (true) {

            data.update();

            report();

            attack();

            attackN();

            move();

            attack();

            attackN();

            getShrine();

            getChest();

            useArtifact();

            //enterDungeon();

            uc.yield();
        }

    }


    void abilityOne(){

        UnitInfo[] enemies = uc.senseUnits(8,data.allyTeam, true);
        UnitInfo[] allies = uc.senseUnits(8,data.allyTeam);


        if(enemies.length > 3 && allies.length == 0) {
            if (uc.getInfo().getLevel() < 2) {
                if (uc.canLevelUp() && uc.getReputation() > data.barbarianLvlThreshold) uc.levelUp();
                else if (uc.canUseFirstAbility(uc.getLocation())) uc.useFirstAbility(uc.getLocation());
                return;
            } else if (uc.canUseFirstAbility(uc.getLocation())) uc.useFirstAbility(uc.getLocation());
        }




    }

}