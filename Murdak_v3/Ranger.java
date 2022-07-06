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


    public void attack(){

        UnitInfo[] enemiesAround = uc.senseUnits(data.enemyTeam);
        Location target = uc.getLocation();
        float priority = 0;

        float attack = uc.getType().getStat(UnitStat.ATTACK);

        for (UnitInfo unit : enemiesAround){

            if(!uc.canAttack(unit.getLocation()) ) continue;

            float enemyMaxHealth = unit.getType().getStat(UnitStat.MAX_HEALTH);

            float unitPriority = targetPriority(unit);
            //prioriza atacar a matar
            if(unit.getHealth() <= attack) unitPriority += 50;
            else unitPriority = unitPriority * (float)(enemyMaxHealth /unit.getHealth());

            if (unitPriority > priority){
                priority = unitPriority;
                target = unit.getLocation();
            }
        }

        if (!target.isEqual(uc.getLocation()) ) uc.attack(target);

    }

    public void attackN(){

        UnitInfo[] enemiesAround = uc.senseUnits(Team.NEUTRAL);
        Location target = uc.getLocation();
        float priority = 0;

        float attack = uc.getType().getStat(UnitStat.ATTACK);

        for (UnitInfo unit : enemiesAround){

            if(!uc.canAttack(unit.getLocation()) ) continue;

            float enemyMaxHealth = unit.getType().getStat(UnitStat.MAX_HEALTH);

            float unitPriority = targetPriority(unit);
            //prioriza atacar a matar
            if(unit.getHealth() <= attack) unitPriority += 50;
            else unitPriority = unitPriority * (float)(enemyMaxHealth /unit.getHealth());

            if (unitPriority > priority){
                priority = unitPriority;
                target = unit.getLocation();
            }
        }

        if (!target.isEqual(uc.getLocation()) ) uc.attack(target);

    }

}
