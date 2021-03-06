package cn.zjc.server.base.impl;

import cn.zjc.mapper.BaseMapper;
import cn.zjc.server.base.BaseServer;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author : zhangjiacheng
 * @ClassName : BaseServerImpl
 * @date : 2018/6/11
 * @Description : 基础server实现类
 */
@Service
@Transactional(rollbackFor = Exception.class)
public abstract class BaseServerImpl<T> implements BaseServer<T> {

    @Resource
    private BaseMapper baseMapper;

    public void setBaseMapper(BaseMapper<T> baseMapper) {
        this.baseMapper = baseMapper;
    }

    /**
     * 查询所有数据
     *
     * @return
     */
    @Override
    public List<T> selectAll() {
        return baseMapper.selectAll();
    }

    /**
     * 根据条件查询数据
     *
     * @return
     */
    @Override
    public List<T> selectAll(T record) {
        return baseMapper.selectAll(record);
    }

    /**
     * 查询数据的条数
     *
     * @param record
     * @return
     */
    @Override
    public int selectCount(T record) {
        return baseMapper.selectCount(record);
    }

    /**
     * 根据id查询数据
     *
     * @param id
     * @return
     */
    @Override
    public T selectByPrimaryKey(Long id) {
        return (T) baseMapper.selectByPrimaryKey(id);
    }

    /**
     * 分页查询
     *
     * @param page
     * @param rows
     * @param record
     * @return
     */
    @Override
    public PageInfo<T> selectPageAll(Integer page, Integer rows, T record) {
        // 设置分页条件
        PageHelper.startPage(page, rows);
        //排序(格式 : 字段 + 顺序)
        PageHelper.orderBy("id desc");
        List<T> list = this.selectAll(record);
        return new PageInfo<>(list);
    }

    /**
     * 新增数据，返回成功的条数
     *
     * @param record
     * @return
     */
    @Override
    public Integer save(T record) {
        return baseMapper.insert(record);
    }

    /**
     * 新增数据，使用不为null的字段，返回成功的条数
     *
     * @param record
     * @return
     */
    @Override
    public Integer saveSelective(T record) {
        return baseMapper.insertSelective(record);
    }

    /**
     * 修改数据，返回成功的条数
     *
     * @param record
     * @return
     */
    @Override
    public Integer update(T record) {
        return baseMapper.updateByPrimaryKey(record);
    }

    /**
     * 修改数据，使用不为null的字段，返回成功的条数
     *
     * @param record
     * @return
     */
    @Override
    public Integer updateSelective(T record) {
        return baseMapper.updateByPrimaryKeySelective(record);
    }

    /**
     * 根据id删除数据
     *
     * @param id
     * @return
     */
    @Override
    public Integer deleteById(Long id) {
        return baseMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据条件做删除
     *
     * @param record
     * @return
     */
    @Override
    public Integer deleteByWhere(T record) {
        return baseMapper.delete(record);
    }
}
