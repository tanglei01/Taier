package com.dtstack.rdos.engine.service.db.dao;

import com.dtstack.rdos.engine.service.db.callback.MybatisSessionCallback;
import com.dtstack.rdos.engine.service.db.callback.MybatisSessionCallbackMethod;
import com.dtstack.rdos.engine.service.db.dataobject.RdosEngineStreamJobRetry;
import com.dtstack.rdos.engine.service.db.mapper.RdosEngineStreamJobRetryMapper;
import org.apache.ibatis.session.SqlSession;

/**
 * @author toutian
 */
public class RdosEngineStreamJobRetryDAO {
	
	public void insert(RdosEngineStreamJobRetry rdosEngineStreamJobRetry) {

        MybatisSessionCallbackMethod.doCallback(new MybatisSessionCallback<Object>(){

            @Override
            public Object execute(SqlSession sqlSession) throws Exception {
            	RdosEngineStreamJobRetryMapper mapper = sqlSession.getMapper(RdosEngineStreamJobRetryMapper.class);
                mapper.insert(rdosEngineStreamJobRetry);
                return null;
            }
        });
	}

}