import json
import random
from datetime import datetime, timedelta
from typing import Dict, List, Tuple

# ────────────────────────────────────────────────────────────────────────────────
# Configuración global
# ────────────────────────────────────────────────────────────────────────────────
ROOT_CATEGORIES = 5        # nº de categorías padre
LEAVES_PER_ROOT = 10       # hojas por cada padre
TOTAL_SELLERS = 1_000
PRODUCTS_PER_SELLER = 1_000
PRODUCTS_FILE = "./items/data/items.json"
CATEGORIES_FILE = "./items/data/categories.json"

# ────────────────────────────────────────────────────────────────────────────────
# Entidades dominio
# ────────────────────────────────────────────────────────────────────────────────
class Category:
    """Representa una categoría (padre o hoja)."""

    def __init__(
        self,
        cat_id: str,
        name: str,
        path_from_root: List[Dict[str, str]],
        children_categories: List[Dict[str, str]],
    ):
        self.id = cat_id
        self.name = name
        self.path_from_root = path_from_root
        self.children_categories = children_categories

    def to_dict(self) -> Dict:
        return {
            "id": self.id,
            "name": self.name,
            "path_from_root": self.path_from_root,
            "children_categories": self.children_categories,
        }


class Product:
    """Entidad que representa un producto."""

    def __init__(
        self,
        product_id: str,
        seller_id: str,
        title: str,
        category_id: str,
        price: float,
        date_created: str,
        last_updated: str,
    ):
        self.id = product_id
        self.seller_id = seller_id
        self.title = title
        self.category_id = category_id
        self.price = price
        self.date_created = date_created
        self.last_updated = last_updated

    def to_dict(self) -> Dict:
        return {
            "seller_id": self.seller_id,
            "title": self.title,
            "category_id": self.category_id,
            "price": self.price,
            "date_created": self.date_created,
            "last_updated": self.last_updated,
        }


# ────────────────────────────────────────────────────────────────────────────────
# Generadores aleatorios
# ────────────────────────────────────────────────────────────────────────────────
class RandomDataGenerator:
    """Genera datos aleatorios para productos y vendedores."""

    PRODUCT_ADJECTIVES = ["Super", "Mega", "Ultra", "Pro", "Basic", "Smart"]
    PRODUCT_ITEMS = ["Laptop", "Phone", "Chair", "Watch", "Headphones", "Shoes"]

    def generate_seller_ids(self, count: int) -> List[str]:
        return [f"SELLER_{i}" for i in range(1, count + 1)]

    # --------------- Fechas ----------------
    @staticmethod
    def generate_dates() -> Tuple[str, str]:
        now = datetime.utcnow()
        days_ago = random.randint(30, 365)
        date_created = now - timedelta(days=days_ago)
        last_updated = date_created + timedelta(days=random.randint(1, 29))
        return (
            date_created.isoformat(timespec="microseconds") + "Z",
            last_updated.isoformat(timespec="microseconds") + "Z",
        )

    # --------------- Precios ----------------
    @staticmethod
    def generate_product_price() -> float:
        return round(random.uniform(10.0, 2_000.0), 2)

    # --------------- Título -----------------
    def generate_product_title(self) -> str:
        return f"{random.choice(self.PRODUCT_ADJECTIVES)} {random.choice(self.PRODUCT_ITEMS)}"


# ────────────────────────────────────────────────────────────────────────────────
# Factories
# ────────────────────────────────────────────────────────────────────────────────
class CategoryFactory:
    """Crea un conjunto acotado de categorías raíz + hoja."""

    def __init__(self, roots: int, leaves_per_root: int):
        self.roots = roots
        self.leaves_per_root = leaves_per_root

    def create_categories(self) -> Tuple[Dict[str, Dict], List[str]]:
        """Devuelve (dict_categorías, lista_ids_hoja)."""
        categories: Dict[str, Dict] = {}
        leaf_ids: List[str] = []

        for r in range(1, self.roots + 1):
            root_id = f"MLA{1000 + r}"
            # Crear root
            root = Category(
                cat_id=root_id,
                name=f"Root Category {r}",
                path_from_root=[{"id": root_id}],
                children_categories=[],
            )
            categories[root_id] = root.to_dict()

            # Crear hojas
            for l in range(1, self.leaves_per_root + 1):
                leaf_id = f"MLA{r:02}{l:02}00"  # p. ej. MLA010100
                leaf = Category(
                    cat_id=leaf_id,
                    name=f"Leaf Category {r}-{l}",
                    path_from_root=[{"id": root_id}, {"id": leaf_id}],
                    children_categories=[],
                )
                categories[leaf_id] = leaf.to_dict()
                leaf_ids.append(leaf_id)
                # Registrar relación padre→hijo
                categories[root_id]["children_categories"].append({"id": leaf_id})

        return categories, leaf_ids


class ProductFactory:
    """Crea instancias de Product usando categorías hoja existentes."""

    def __init__(self, data_gen: RandomDataGenerator, leaf_category_ids: List[str]):
        self.data_gen = data_gen
        self.leaf_category_ids = leaf_category_ids

    def create_product(self, product_index: int, seller_id: str) -> Product:
        product_id = f"MLA{product_index}"
        title = self.data_gen.generate_product_title()
        category_id = random.choice(self.leaf_category_ids)
        price = self.data_gen.generate_product_price()
        date_created, last_updated = self.data_gen.generate_dates()
        return Product(
            product_id,
            seller_id,
            title,
            category_id,
            price,
            date_created,
            last_updated,
        )


# ────────────────────────────────────────────────────────────────────────────────
# Repositorios / persistencia
# ────────────────────────────────────────────────────────────────────────────────
class JsonRepository:
    """Base para persistir diccionarios en archivos JSON."""

    def __init__(self):
        self._data: Dict[str, Dict] = {}

    def add(self, obj_id: str, obj_dict: Dict) -> None:
        self._data[obj_id] = obj_dict

    def bulk_add(self, data: Dict[str, Dict]) -> None:
        self._data.update(data)

    def save(self, filename: str) -> None:
        with open(filename, "w", encoding="utf-8") as f:
            json.dump(self._data, f, indent=2, ensure_ascii=False)

    @property
    def size(self) -> int:
        return len(self._data)


# ────────────────────────────────────────────────────────────────────────────────
# Seeder principal
# ────────────────────────────────────────────────────────────────────────────────
class ProductSeeder:
    """Genera vendedores, categorías y productos, y los persiste en JSON."""

    def __init__(
        self,
        num_sellers: int,
        products_per_seller: int,
        roots: int,
        leaves_per_root: int,
        items_file: str,
        categories_file: str,
    ):
        self.num_sellers = num_sellers
        self.products_per_seller = products_per_seller
        self.items_file = items_file
        self.categories_file = categories_file

        # Generadores / fábricas
        self.data_gen = RandomDataGenerator()
        category_factory = CategoryFactory(roots, leaves_per_root)
        self.categories_dict, self.leaf_ids = category_factory.create_categories()
        self.product_factory = ProductFactory(self.data_gen, self.leaf_ids)

        # Repositorios
        self.product_repo = JsonRepository()
        self.category_repo = JsonRepository()
        self.category_repo.bulk_add(self.categories_dict)

    def run(self) -> None:
        seller_ids = self.data_gen.generate_seller_ids(self.num_sellers)
        product_index = 1

        for seller_id in seller_ids:
            for _ in range(self.products_per_seller):
                product = self.product_factory.create_product(product_index, seller_id)
                self.product_repo.add(product.id, product.to_dict())
                product_index += 1

        # Guardar resultados
        self.category_repo.save(self.categories_file)
        self.product_repo.save(self.items_file)

        print(
            f"✅ {self.categories_file}: {self.category_repo.size} categorías\n"
            f"✅ {self.items_file}: {self.product_repo.size} productos"
        )


# ────────────────────────────────────────────────────────────────────────────────
# Ejecución
# ────────────────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    seeder = ProductSeeder(
        num_sellers=TOTAL_SELLERS,
        products_per_seller=PRODUCTS_PER_SELLER,
        roots=ROOT_CATEGORIES,
        leaves_per_root=LEAVES_PER_ROOT,
        items_file=PRODUCTS_FILE,
        categories_file=CATEGORIES_FILE,
    )
    seeder.run()
