/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.util;


import org.apache.shiro.cache.ehcache.EhCacheManager;

/**
 * ChatNoir cache manager which uses EHCache.
 * New instances of this class will reuse the same singleton cache manager.
 */
public class CacheManager extends EhCacheManager
{
    public CacheManager()
    {
        setCacheManager(net.sf.ehcache.CacheManager.create());
    }
}
