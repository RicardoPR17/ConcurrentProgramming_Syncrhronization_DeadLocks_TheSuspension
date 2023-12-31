/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hcadavid
 */
public class HostBlackListsValidator {

    private static final int BLACK_LIST_ALARM_COUNT = 5;
    private LinkedList<Integer> blackListOcurrences;
    private ArrayList<Thread> threadsList = new ArrayList<>();

    /**
     * Check the given host's IP address in all the available black lists,
     * and report it as NOT Trustworthy when such IP was reported in at least
     * BLACK_LIST_ALARM_COUNT lists, or as Trustworthy in any other case. The search
     * is divided by the number of threads, each of them search in a range of the
     * servers. When all the threads complete his part or the alarm count has been
     * reach, the next step is validate if the given host's IP address is
     * turstworthy or not. The search is not exhaustive: When the number of
     * occurrences is equal to BLACK_LIST_ALARM_COUNT, the search is finished, the
     * host reported as NOT Trustworthy, and the list of the five blacklists
     * returned.
     * 
     * @param ipaddress suspicious host's IP address.
     * @param n         number of threads
     * @return Blacklists numbers where the given host's IP address was found.
     */
    public List<Integer> checkHost(String ipaddress, int n) {

        blackListOcurrences = new LinkedList<>();

        int ocurrencesCount = 0;

        HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();

        int checkedListsCount = 0;

        int inf_limit = 1;

        int sup_limit = skds.getRegisteredServersCount();

        for (int i = 0; i < n; i++) {
            ThreadHostBlackListValidator thread = new ThreadHostBlackListValidator(ipaddress,
                    inf_limit + (i * (sup_limit - inf_limit) / n), inf_limit + ((i + 1) * (sup_limit - inf_limit) / n),
                    blackListOcurrences);

            threadsList.add(thread);
        }

        for (Thread j : threadsList) {
            j.start();
        }

        while (areThreadsAlive()) {
        }

        for (Thread l : threadsList) {
            ThreadHostBlackListValidator threadAux = (ThreadHostBlackListValidator) l;
            ocurrencesCount += threadAux.getOcurrences();
            checkedListsCount += threadAux.getCheckedList();
        }

        if (ocurrencesCount >= BLACK_LIST_ALARM_COUNT) {
            skds.reportAsNotTrustworthy(ipaddress);
        } else {
            skds.reportAsTrustworthy(ipaddress);
        }

        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}",
                new Object[] { checkedListsCount, skds.getRegisteredServersCount() });

        return blackListOcurrences;
    }

    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());

    /**
     * Check if the created threads are stopped or not
     * 
     * @return false if at least one thread is stopped, true otherwise
     */
    private boolean areThreadsAlive() {
        for (Thread t : threadsList) {
            if (t.getState() == Thread.State.TERMINATED) {
                for (Thread l : threadsList) {
                    l.stop();
                }
                return false;
            }
        }
        return true;
    }

}