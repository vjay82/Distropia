/*--------------------------------------------------------------------------
 --
 --   @(#) Datei   : [item]
 --   @(#) Version : [version]
 --   @(#) Status  : [state]
 --   @(#) Datum   : [modtime]
 --   @(#) Pfad    : [viewpath]
 --   @(#) Package : [package]
 --
 ----------------------------------------------------------------------------
 --
 --   Aenderungen:
 --
 --   LNR        Datum       Name            SCR/Beschreibung
 --
 --   000        22.02.2007  Gronau/NextiraOne      Erstellung
 --
 --------------------------------------------------------------------------*/
package org.distropia.server.platformspecific;


/**
 * DOCUMENT_ME
 *
 * @author $author$
 * @version $Revision$
  */
public class TimeOut
{
    long timeOut;

    /**
     * Erzeugt neues TimeOut-Objekt.
     *
     * @param time time
     */
    public TimeOut(final long time)
    {
        super();

        if (time == 0)
        {
            timeOut = 0;
        }
        else if (time == 1)
        {
            timeOut = 1;
        }
        else
        {
            timeOut = System.currentTimeMillis() + time;

            if (timeOut < System.currentTimeMillis())
            {
                timeOut = time;
            }
        }
    }

    /**
     * DOCUMENT_ME
     *
     * @return DOCUMENT_ME
     */
    public boolean isTimeOut()
    {
        if (timeOut == 0)
        {
            return false;
        }
        else
        {
            return System.currentTimeMillis() > timeOut;
        }
    }
}
