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
        }, (20*3600L), 20*3600L); // 1 hour delay, 1 hour period        
    }
}
