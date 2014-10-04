<?php

class Lorem
{
    /** @var \Kdyby\Doctrine\EntityDao */
    protected $fooDao;

    public function __construct(\Kdyby\Doctrine\EntityManager $entityManager)
    {
        $this->fooDao = $entityManager->getDao(FooEntity::class);
    }

    /**
     * @return FooEntity|object
     */
    public function doSth()
    {
        return $this->fooDao->find();
    }
}



/** @var \Kdyby\Doctrine\EntityManager $entityManager */
$entityDao = $entityManager->getDao(FooEntity::class);
$entityDao->find()->id;


foreach($entityDao->findAll() as $entity) {
    $entity->id;
}

$result = $entityDao->fetch();
$result->applyPaginator();
foreach($result as $entity) {
    $entity->id;
}