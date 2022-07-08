package Murdak_v3;

import aic2022.user.*;

public class Ranger extends CombatUnit {

    public Ranger (UnitController _uc) {
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

            abilityOne();

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

        UnitInfo[] units = uc.senseUnits(36,data.allyTeam, true);

        for (UnitInfo u : units){
            if (u.getType() == UnitType.BARBARIAN || uc.getType() == UnitType.EXPLORER
                || (u.getType() == UnitType.KNIGHT && u.getLevel() < 2) ){
                if(uc.getInfo().getLevel() < 2){
                    if(uc.canLevelUp() && uc.getReputation() > data.rangerLvlThreshold ) uc.levelUp();
                    return;
                }
                else if (uc.canUseFirstAbility( u.getLocation() ) ){
                    uc.useFirstAbility(u.getLocation());
                    return;
                }

            }

        }

    }




}
