CREATE TABLE IF NOT EXISTS `ClientMessages` (
  `id` int(10) AUTO_INCREMENT,
  `name` varchar(50),
  `secondname` varchar(50),
  `message` text,
  `date` TIMESTAMP,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `SeverMessages` (
  `id` int(10) AUTO_INCREMENT,
  `name` varchar(50),
  `message` text,
  `date` TIMESTAMP,
  PRIMARY KEY (`id`)
);
