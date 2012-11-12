package com.github.CubieX.Assignment;

public class ASSSchedulerHandler
{

    private Assignment aInst = null;

    public ASSSchedulerHandler(Assignment aInst)
    {
        this.aInst = aInst;
    }

    public void startCleanupScheduler_SyncRep()
    {      
        aInst.getServer().getScheduler().scheduleSyncRepeatingTask(aInst, new Runnable()
        {
            public void run()
            {
                aInst.cleanupAssignmentsInDB(null);
            }
        }, (20*10800L), 20*10800L); // 3 hour delay, 3 hour period        
    }
}
