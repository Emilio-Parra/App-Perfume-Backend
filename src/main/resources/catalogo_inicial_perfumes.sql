-- PERFUMES STORE - CATÁLOGO INICIAL EXTRAÍDO DE LA EVALUACIÓN 2
-- Fuente: eva2.zip / public/data/productos.json y src/service/categoriasService.js
-- 27 productos y 3 categorias. NO incluye usuarios, cuentas, contraseñas ni pedidos.
-- IMPORTANTE: ejecutar primero Spring Boot para crear las tablas con Hibernate.
-- Solo inserta registros que no existan por nombre de categoría o por marca+modelo.
-- No borra, modifica ni reemplaza productos existentes.


SET NAMES utf8mb4;
START TRANSACTION;

-- Categorías
INSERT INTO `categorias` (`nombre`, `descripcion`)
SELECT 'arabe', 'Fragancias árabes, intensas y duraderas'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `categorias` WHERE `nombre` = 'arabe');

INSERT INTO `categorias` (`nombre`, `descripcion`)
SELECT 'nicho', 'Perfumería de nicho y ediciones limitadas'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `categorias` WHERE `nombre` = 'nicho');

INSERT INTO `categorias` (`nombre`, `descripcion`)
SELECT 'diseñador', 'Casas de moda y perfumería de diseñador'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `categorias` WHERE `nombre` = 'diseñador');

-- Productos; stock inicial reconstruido usando la fórmula del servicio de EVA 2
-- Cuando la imagen comienza con /img/, se sirve desde public/img/ del FRONTEND.

-- 01. Hawas - Malibu
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Hawas', 'Malibu', 'arabe', 'hombre', 39990, '100 ml', '/img/arabe/images.jpg', 'Fragancia aromática y frutal de la línea Hawas de Rasasi, lanzada en 2025. Abre con piña, naranja y pomelo; en el corazón aparecen raíz de lirio, ámbar y lavanda, y cierra con haba tonka, almizcle, cachemira y pachulí. Fresca y luminosa, pensada para el día a día y los climas cálidos.', 15, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Hawas' AND `modelo` = 'Malibu');

-- 02. Rayhaan - Pacific Aura
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Rayhaan', 'Pacific Aura', 'arabe', 'hombre', 63636, '100 ml', '/img/arabe/images (1).jpg', 'Fragancia aromática acuática lanzada en 2025. Su salida combina mandarina, menta, cidra, bergamota, grosella negra y cilantro; el corazón mezcla albahaca, zanahoria y rosa, y el fondo se apoya en higo, ambroxan y ámbar. Una opción fresca y marina, ideal para el uso diario y el verano.', 0, 9
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Rayhaan' AND `modelo` = 'Pacific Aura');

-- 03. Lattafa - Eclaire
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Lattafa', 'Eclaire', 'arabe', 'mujer', 29990, '105 ml', '/img/arabe/images (2).jpg', 'Fragancia gourmand floral-frutal lanzada en 2024. Abre con caramelo, azúcar y leche; en el corazón se funden la miel y las flores blancas, y termina en vainilla, praliné y almizcle. Dulce, cremosa y envolvente, para quienes disfrutan de los aromas golosos.', 20, 16
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Lattafa' AND `modelo` = 'Eclaire');

-- 04. Afnan - 9 AM
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Afnan', '9 AM', 'arabe', 'hombre', 69120, '100 ml', '/img/arabe/images (3).jpg', 'Fragancia árabe de la casa Afnan, de carácter fresco y aromático, pensada para empezar la mañana. Combina un inicio luminoso con matices de lavanda y un fondo suave de vainilla y almizcle. Ligera, limpia y fácil de usar en oficina y clima templado.', 10, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Afnan' AND `modelo` = '9 AM');

-- 05. Armaf - Club The Nuit Untold
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Armaf', 'Club The Nuit Untold', 'arabe', 'unisex', 39990, '100 ml', '/img/arabe/images (4).jpg', 'Fragancia de Armaf de perfil ambarado y especiado, con un inicio brillante y un fondo cálido de ámbar, vainilla y maderas. Intensa y de buena proyección, pensada para salidas nocturnas y climas fríos.', 30, 12
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Armaf' AND `modelo` = 'Club The Nuit Untold');

-- 06. Lattafa - Khamrah
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Lattafa', 'Khamrah', 'arabe', 'unisex', 69990, '100 ml', '/img/arabe/images (5).jpg', 'Fragancia oriental gourmand de Lattafa. Abre con canela, nuez moscada y bergamota; en el corazón aparecen dátiles, praliné y flores blancas, y cierra con vainilla, haba tonka, benjuí y ámbar. Cálida, dulce y especiada, ideal para otoño e invierno.', 0, 19
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Lattafa' AND `modelo` = 'Khamrah');

-- 07. French Avenue - Amber Empire
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'French Avenue', 'Amber Empire', 'arabe', 'unisex', 93000, '100 ml', '/img/arabe/images (6).jpg', 'Fragancia ambarada de French Avenue, de carácter cálido, dulce y envolvente. Su fondo gira en torno al ámbar y la vainilla, con un toque amaderado que le da profundidad. Pensada para la noche y para quienes buscan una estela intensa.', 0, 8
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'French Avenue' AND `modelo` = 'Amber Empire');

-- 08. Lattafa - Khamrah Waha
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Lattafa', 'Khamrah Waha', 'arabe', 'mujer', 39900, '100 ml', '/img/arabe/images (7).jpg', 'Fragancia de la línea Khamrah de Lattafa con un perfil dulce y especiado de aire gourmand. Mantiene la calidez característica de la familia, con notas cremosas y un fondo suave y envolvente. Pensada para uso femenino en tardes y noches frescas.', 25, 15
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Lattafa' AND `modelo` = 'Khamrah Waha');

-- 09. Dior - Sauvage EDP
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Dior', 'Sauvage EDP', 'diseñador', 'hombre', 148800, '100 ml', '/img/disenador/descargar.webp', 'Versión Eau de Parfum del clásico de Dior, creada por François Demachy en 2018. Abre con bergamota y pimienta, sigue con lavanda y termina en vainilla y haba tonka. Fresca, especiada y de gran proyección, muy versátil.', 0, 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Dior' AND `modelo` = 'Sauvage EDP');

-- 10. Jean Paul Gaultier - Le Beau Paradise Garden
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Jean Paul Gaultier', 'Le Beau Paradise Garden', 'diseñador', 'hombre', 133190, '125 ml', '/img/disenador/descargar (1).webp', 'Fragancia de Jean Paul Gaultier de la línea Le Beau, en su edición Paradise Garden. Propone un perfil fresco, verde y tropical, con notas frutales y un fondo amaderado suave. Pensada para el verano y los días cálidos.', 0, 11
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Jean Paul Gaultier' AND `modelo` = 'Le Beau Paradise Garden');

-- 11. Paco Rabanne - One Million
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Paco Rabanne', 'One Million', 'diseñador', 'hombre', 66990, '100 ml', '/img/disenador/images.jpg', 'Clásico de Paco Rabanne lanzado en 2008. Abre con pomelo, mandarina sanguínea y menta; en el corazón aparecen canela, rosa y especias, y cierra con cuero, ámbar, maderas y pachulí. Dulce, especiada y llamativa, ideal para salidas de noche.', 15, 18
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Paco Rabanne' AND `modelo` = 'One Million');

-- 12. Giorgio Armani - Acqua Di Gio Profondo
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Giorgio Armani', 'Acqua Di Gio Profondo', 'diseñador', 'hombre', 119990, '100 ml', '/img/disenador/OIP.webp', 'Fragancia acuática y aromática de Giorgio Armani lanzada en 2020. Combina bergamota y notas marinas con romero y un fondo mineral con pachulí. Fresca, profunda y limpia, pensada para el verano y el uso diario.', 0, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Giorgio Armani' AND `modelo` = 'Acqua Di Gio Profondo');

-- 13. Giorgio Armani - Acqua Di Gio
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Giorgio Armani', 'Acqua Di Gio', 'diseñador', 'hombre', 106400, '100 ml', '/img/disenador/OIP (1).webp', 'Clásico acuático de Giorgio Armani lanzado en 1996. Abre con limón, lima y bergamota; en el corazón aparecen notas marinas, jazmín y romero, y cierra con cedro, pachulí y almizcle blanco. Una fragancia fresca y atemporal, perfecta para el calor.', 10, 14
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Giorgio Armani' AND `modelo` = 'Acqua Di Gio');

-- 14. Jean Paul Gaultier - Le Beau Parfum
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Jean Paul Gaultier', 'Le Beau Parfum', 'diseñador', 'hombre', 144990, '125 ml', '/img/disenador/OIP (2).webp', 'Fragancia de Jean Paul Gaultier de la línea Le Beau, en su versión Parfum. Destaca por su apertura de bergamota y coco, un corazón cremoso y un fondo con haba tonka y maderas. Dulce, tropical y envolvente.', 0, 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Jean Paul Gaultier' AND `modelo` = 'Le Beau Parfum');

-- 15. Paco Rabanne - One Million Royal
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Paco Rabanne', 'One Million Royal', 'diseñador', 'hombre', 95000, '100 ml', '/img/disenador/OIP (3).webp', 'Versión Royal de la familia One Million de Paco Rabanne. Mantiene el espíritu especiado y ambarado de la línea, con matices de cuero y maderas en el fondo. Intensa, lujosa y de gran estela, pensada para la noche.', 0, 10
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Paco Rabanne' AND `modelo` = 'One Million Royal');

-- 16. Paco Rabanne - Invictus Parfume
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Paco Rabanne', 'Invictus Parfume', 'diseñador', 'hombre', 133080, '100 ml', '/img/disenador/OIP (4).webp', 'Versión Parfum de la línea Invictus de Paco Rabanne. Conserva su carácter fresco y deportivo, pero con una concentración mayor y un fondo más ambarado y amaderado. Potente y duradera, ideal para el día a día.', 0, 17
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Paco Rabanne' AND `modelo` = 'Invictus Parfume');

-- 17. Versace - Eros Flame
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Versace', 'Eros Flame', 'diseñador', 'hombre', 55000, '100 ml', '/img/disenador/OIP (5).webp', 'Fragancia de Versace de la línea Eros, en su edición Flame. Abre con mandarina, limón y pimienta negra; en el corazón destacan romero, geranio y pimienta, y cierra con vainilla, haba tonka, sándalo y pachulí. Cálida, especiada y seductora.', 20, 6
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Versace' AND `modelo` = 'Eros Flame');

-- 18. Armani - Stronger With You Intensely
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Armani', 'Stronger With You Intensely', 'diseñador', 'hombre', 74900, '100 ml', '/img/disenador/OIP (6).webp', 'Fragancia de Giorgio Armani de la línea Stronger With You, en su versión Intensely. Combina canela, lavanda y castaña asada con un fondo de vainilla, ámbar y maderas. Dulce, cálida y cremosa, ideal para otoño e invierno.', 12, 13
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Armani' AND `modelo` = 'Stronger With You Intensely');

-- 19. Xerjoff - Erba Pura
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Xerjoff', 'Erba Pura', 'nicho', 'unisex', 95000, '100 ml', '/img/nicho/images.jpg', 'Fragancia de nicho de Xerjoff lanzada en 2019. Abre con naranja siciliana, limón y bergamota, con un corazón de frutas, y cierra con almizcle, vainilla y ámbar. Frutal, luminosa y muy dulce, de gran proyección.', 0, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Xerjoff' AND `modelo` = 'Erba Pura');

-- 20. L'Artisan Parfumeur - Mirabilis
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'L''Artisan Parfumeur', 'Mirabilis', 'nicho', 'unisex', 98000, '100 ml', '/img/nicho/images (1).jpg', 'Perfume de nicho de la casa francesa L''Artisan Parfumeur. Una propuesta elegante y envolvente, con un fondo cálido y suave que se siente refinado sobre la piel. Pensada para quienes buscan una fragancia distinta y de carácter artesanal.', 15, 9
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'L''Artisan Parfumeur' AND `modelo` = 'Mirabilis');

-- 21. Parfums de Marly - Layton
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Parfums de Marly', 'Layton', 'nicho', 'hombre', 102000, '100 ml', '/img/nicho/images (2).jpg', 'Fragancia de nicho de Parfums de Marly lanzada en 2016. Abre con manzana, lavanda, bergamota y mandarina; en el corazón aparecen geranio, violeta y jazmín, y cierra con vainilla, cardamomo, sándalo, pimienta, guayaco y pachulí. Elegante, versátil y de excelente proyección.', 0, 16
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Parfums de Marly' AND `modelo` = 'Layton');

-- 22. Ex Nihilo - Blue Talisman
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Ex Nihilo', 'Blue Talisman', 'nicho', 'unisex', 280000, '100 ml', '/img/nicho/images(blue).jpg', 'Fragancia de nicho de la casa francesa Ex Nihilo. Propone un perfil fresco y luminoso, con matices florales y un fondo amaderado limpio. Elegante y ligera, ideal para el día y los climas templados.', 0, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Ex Nihilo' AND `modelo` = 'Blue Talisman');

-- 23. Tom Ford - Ombre Leather
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Tom Ford', 'Ombre Leather', 'nicho', 'unisex', 110000, '100 ml', '/img/nicho/images (12).jpg', 'Fragancia de la colección privada de Tom Ford lanzada en 2018. Abre con cardamomo, en el corazón destacan el cuero y el jazmín sambac, y cierra con ámbar, musgo y pachulí. Cálida, amaderada y sofisticada, con un marcado carácter de cuero.', 0, 12
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Tom Ford' AND `modelo` = 'Ombre Leather');

-- 24. Andrew's - N18
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Andrew''s', 'N18', 'nicho', 'hombre', 99000, '100 ml', '/img/nicho/images (5).jpg', 'Fragancia de nicho de Andrew''s, la línea N18. Propone un perfil masculino, elegante y sobrio, pensado para el uso diario y para ocasiones formales.', 20, 19
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Andrew''s' AND `modelo` = 'N18');

-- 25. Auraa - Selene
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Auraa', 'Selene', 'nicho', 'mujer', 115000, '100 ml', '/img/nicho/images (6).jpg', 'Fragancia de nicho de la marca Auraa, en su versión Selene. Con un carácter delicado y femenino, pensada para quienes prefieren aromas suaves y elegantes, ideales para el día y la tarde.', 0, 8
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Auraa' AND `modelo` = 'Selene');

-- 26. Guisada - Guisada
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Guisada', 'Guisada', 'nicho', 'unisex', 120000, '100 ml', '/img/nicho/images (11).jpg', 'Fragancia de nicho de la marca Guisada. Un perfume distintivo y de carácter, pensado para quienes buscan un aroma diferente al de las marcas más comerciales.', 0, 15
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Guisada' AND `modelo` = 'Guisada');

-- 27. Creed - Aventus
INSERT INTO `productos` (`marca`, `modelo`, `tipo`, `genero`, `precio`, `volumen`, `imagenes`, `descripcion`, `descuento`, `stock`)
SELECT 'Creed', 'Aventus', 'nicho', 'hombre', 125000, '100 ml', '/img/nicho/images10.jpg', 'Fragancia de nicho de Creed, considerada una de las más icónicas del mundo. Abre con piña, bergamota, manzana y grosella negra; en el corazón aparecen abedul, jazmín y rosa, y cierra con almizcle, musgo de roble, ámbar gris y vainilla. Frutal, ahumada y elegante, símbolo del éxito.', 0, 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `productos`
                  WHERE `marca` = 'Creed' AND `modelo` = 'Aventus');

COMMIT;

-- Comprobación final
SELECT `tipo`, COUNT(*) AS `cantidad` FROM `productos` GROUP BY `tipo` ORDER BY `tipo`;
SELECT COUNT(*) AS `total_productos` FROM `productos`;
SELECT `nombre` FROM `categorias` ORDER BY `nombre`;
