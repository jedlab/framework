package com.jedlab.framework.web;

import java.util.Optional;

/**
 * @author Omid Pourhadi
 *
 */
public class Pagination
{

    protected static final int INITIAL_PAGE = 0;
    protected static final int INITIAL_PAGE_SIZE = 200;

    private final Optional<Integer> page;
    private final Optional<Integer> pageSize;
    
    

    public Pagination(Optional<Integer> page, Optional<Integer> pageSize) {
		this.page = page;
		this.pageSize = pageSize;
	}

	public int getEvalPageSize()
    {
        return pageSize.orElse(INITIAL_PAGE_SIZE);
    }

    public int getEvalPage()
    {
        Integer orElsePage = page.orElse(0);
        int evalPage = (orElsePage < 1) ? INITIAL_PAGE : orElsePage - 1;
        return evalPage;
    }

}