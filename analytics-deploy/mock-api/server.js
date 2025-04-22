const express = require('express');
const app = express();
const port = 3000;

// Sample data to randomize from
const makes = ['BMW', 'Audi', 'Toyota', 'Mercedes', 'Tesla'];
const models = {
  BMW: ['3 Series', '5 Series', 'X5'],
  Audi: ['A4', 'A6', 'Q7'],
  Toyota: ['Camry', 'Corolla', 'RAV4'],
  Mercedes: ['C-Class', 'E-Class', 'GLC'],
  Tesla: ['Model 3', 'Model S', 'Model X']
};
const cities = ['Київ', 'Львів', 'Харків', 'Дніпро', 'Одеса'];
const currencyRates = { USD: 1, EUR: 0.92, UAH: 39 };

// Helper to get random item
const rand = (arr) => arr[Math.floor(Math.random() * arr.length)];

// Generate random price
const generatePrice = () => {
  const usd = Math.floor(15000 + Math.random() * 30000);
  return {
    USD: usd,
    EUR: Math.floor(usd * currencyRates.EUR),
    UAH: Math.floor(usd * currencyRates.UAH),
  };
};

// Generate mock VIN
const generateVIN = () => `WBA${Math.floor(100000 + Math.random() * 900000)}XYZ123`;

// Generate random car IDs for search results
const generateCarIds = (count = 50) => {
  const ids = [];
  // Generate IDs in the format seen in the real API (33000000-36000000 range)
  const baseNumbers = [33, 34, 35, 36];
  
  for (let i = 0; i < count; i++) {
    const base = baseNumbers[Math.floor(Math.random() * baseNumbers.length)];
    // Return integers for autoId to match Avro schema
    ids.push(`${base}${Math.floor(100000 + Math.random() * 899999)}`);
  }
  return ids;
};

// Generate search result data
const generateSearchResultData = (ids) => {
  return ids.map(id => ({
    id: id,
    type: "UsedAuto"
  }));
};

// Add fuel type mappings
const fuelTypes = [
  { id: 1, name: 'Бензин', nameEng: 'Petrol' },
  { id: 2, name: 'Дизель', nameEng: 'Diesel' },
  { id: 3, name: 'Газ', nameEng: 'Gas' },
  { id: 4, name: 'Газ / Бензин', nameEng: 'Gas/Petrol' },
  { id: 5, name: 'Гібрид', nameEng: 'Hybrid' },
  { id: 6, name: 'Електро', nameEng: 'Electric' },
  { id: 7, name: 'Інше', nameEng: 'Other' },
  { id: 8, name: 'Газ метан', nameEng: 'Methane' },
  { id: 9, name: 'Газ пропан-бутан', nameEng: 'Propane-Butane' }
];

// Add drive type mappings
const driveTypes = [
  { id: 1, name: 'Передній', nameEng: 'Front-wheel drive' },
  { id: 2, name: 'Задній', nameEng: 'Rear-wheel drive' },
  { id: 3, name: 'Повний', nameEng: 'All-wheel drive' }
];

// Add gearbox type mappings
const gearboxTypes = [
  { id: 1, name: 'Ручна / Механіка', nameEng: 'Manual' },
  { id: 2, name: 'Автомат', nameEng: 'Automatic' },
  { id: 3, name: 'Типтронік', nameEng: 'Tiptronic' },
  { id: 4, name: 'Робот', nameEng: 'Semi-automatic' },
  { id: 5, name: 'Варіатор', nameEng: 'CVT' }
];

// Add body type mappings
const bodyTypes = [
  { id: 1, name: 'Позашляховик / Кросовер', nameEng: 'SUV' },
  { id: 2, name: 'Універсал', nameEng: 'Estate' },
  { id: 3, name: 'Седан', nameEng: 'Sedan' },
  { id: 4, name: 'Хетчбек', nameEng: 'Hatchback' },
  { id: 5, name: 'Купе', nameEng: 'Coupe' },
  { id: 6, name: 'Кабріолет', nameEng: 'Convertible' },
  { id: 7, name: 'Мінівен', nameEng: 'Minivan' },
  { id: 8, name: 'Пікап', nameEng: 'Pickup' },
  { id: 9, name: 'Ліфтбек', nameEng: 'Liftback' }
];

// Add category mappings
const categories = [
  { id: 1, name: 'Легкові', nameEng: 'Cars' },
  { id: 2, name: 'Мото', nameEng: 'Motorcycles' },
  { id: 3, name: 'Водний транспорт', nameEng: 'Watercraft' },
  { id: 4, name: 'Спецтехніка', nameEng: 'Special machinery' },
  { id: 5, name: 'Причепи', nameEng: 'Trailers' },
  { id: 6, name: 'Вантажівки', nameEng: 'Trucks' },
  { id: 7, name: 'Автобуси', nameEng: 'Buses' },
  { id: 8, name: 'Автобудинки', nameEng: 'Motorhomes' }
];

// Add region mappings
const regions = [
  { id: 10, name: 'Київська', regionName: 'Київ', nameEng: 'Kyiv region', regionNameEng: 'Kyiv' },
  { id: 1, name: 'Вінницька', regionName: 'Вінниця', nameEng: 'Vinnytsia region', regionNameEng: 'Vinnytsia' },
  { id: 2, name: 'Житомирська', regionName: 'Житомир', nameEng: 'Zhytomyr region', regionNameEng: 'Zhytomyr' },
  { id: 3, name: 'Тернопільська', regionName: 'Тернопіль', nameEng: 'Ternopil region', regionNameEng: 'Ternopil' }
];

// Add make and model mappings
const carMakes = [
  { id: 9, name: 'BMW', nameEng: 'BMW' },
  { id: 6, name: 'Audi', nameEng: 'Audi' },
  { id: 79, name: 'Toyota', nameEng: 'Toyota' },
  { id: 48, name: 'Mercedes-Benz', nameEng: 'Mercedes-Benz' },
  { id: 2233, name: 'Tesla', nameEng: 'Tesla' }
];

const carModels = {
  9: [ // BMW
    { id: 96, name: "3 Series", nameEng: "3 Series" },
    { id: 97, name: "5 Series", nameEng: "5 Series" },
    { id: 102, name: "X5", nameEng: "X5" }
  ],
  6: [ // Audi
    { id: 47, name: "A4", nameEng: "A4" },
    { id: 49, name: "A6", nameEng: "A6" },
    { id: 2032, name: "Q7", nameEng: "Q7" }
  ],
  79: [ // Toyota
    { id: 698, name: "Camry", nameEng: "Camry" },
    { id: 703, name: "Corolla", nameEng: "Corolla" },
    { id: 721, name: "RAV4", nameEng: "RAV4" }
  ],
  48: [ // Mercedes
    { id: 387, name: "C-Class", nameEng: "C-Class" },
    { id: 389, name: "E-Class", nameEng: "E-Class" },
    { id: 60344, name: "GLC", nameEng: "GLC" }
  ],
  2233: [ // Tesla
    { id: 43668, name: "Model 3", nameEng: "Model 3" },
    { id: 34542, name: "Model S", nameEng: "Model S" },
    { id: 35257, name: "Model X", nameEng: "Model X" }
  ]
};

app.get('/auto/info', (req, res) => {
  // Get make from carMakes
  const makeMeta = rand(carMakes);
  // Get model from carModels using the make ID
  const modelMeta = rand(carModels[makeMeta.id]);
  
  const prices = generatePrice();
  const region = rand(regions);
  const year = 2000 + Math.floor(Math.random() * 23);
  // Convert autoId to integer if it comes as a query parameter
  let autoId = req.query.auto_id || Math.floor(35000000 + Math.random() * 2000000);
  // Ensure autoId is an integer
  autoId = parseInt(autoId);
  
  // Generate subcategory based on category
  const subCategories = {
    1: { id: 2, name: "Універсал", nameEng: "universal" },
    2: { id: 2, name: "Мотоцикли", nameEng: "motocikly" },
    3: { id: 3, name: "Човни", nameEng: "lodki" },
    // Default for other categories
    4: { id: 4, name: "Спецтехніка", nameEng: "spectehnika" },
    5: { id: 5, name: "Причепи", nameEng: "pritsepy" },
    6: { id: 6, name: "Вантажівки", nameEng: "gruzoviki" },
    7: { id: 7, name: "Автобуси", nameEng: "avtobusy" },
    8: { id: 8, name: "Автобудинки", nameEng: "avtodomiki" }
  };
  
  // Get random types
  const fuelType = rand(fuelTypes);
  const driveType = rand(driveTypes);
  const gearboxType = rand(gearboxTypes);
  const bodyType = rand(bodyTypes);
  const category = rand(categories);
  const subCategory = subCategories[category.id] || subCategories[1];
  
  // Generate random VIN
  const vin = generateVIN();
  const raceInteger = Math.floor(50 + Math.random() * 250);
  
  // Generate random color
  const colors = [
    { eng: "black", hex: "#000000", name: "Чорний" },
    { eng: "white", hex: "#FFFFFF", name: "Білий" },
    { eng: "silver", hex: "#C0C0C0", name: "Сріблястий" },
    { eng: "red", hex: "#FF0000", name: "Червоний" },
    { eng: "blue", hex: "#0000FF", name: "Синій" }
  ];
  const color = rand(colors);
  
  // Generate dates
  const now = new Date();
  const addDate = now.toISOString().split('T').join(' ').substring(0, 19);
  const updateDate = now.toISOString().split('T').join(' ').substring(0, 19);
  
  // Add 60 days for expiration
  const expireDate = new Date(now);
  expireDate.setDate(expireDate.getDate() + 60);
  const expireDateStr = expireDate.toISOString().split('T').join(' ').substring(0, 19);
  
  const mockResponse = {
    markId: makeMeta.id,
    prices: [
      {
        EUR: prices.EUR.toString().replace(/\B(?=(\d{3})+(?!\d))/g, " "),
        UAH: prices.UAH.toString().replace(/\B(?=(\d{3})+(?!\d))/g, " "),
        USD: prices.USD.toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ")
      }
    ],
    infoBarText: "",
    title: `${makeMeta.name} ${modelMeta.name}`,
    videoMessageID: "",
    dontComment: 1,
    updateDate: updateDate,
    expireDate: expireDateStr,
    UAH: prices.UAH,
    exchangePossible: true,
    moderatedAbroad: false,
    infotechReport: {
      vin: ""
    },
    isLeasing: 0,
    cityLocative: `${region.regionName}`,
    linkToView: `/auto_${makeMeta.nameEng.toLowerCase()}_${modelMeta.nameEng.toLowerCase().replace(' ', '_')}_${autoId}.html`,
    technicalChecked: false,
    EUR: prices.EUR,
    modelName: modelMeta.name,
    plateNumberData: {
      text: `AA ${Math.floor(1000 + Math.random() * 9000)} AA`
    },
    soldDate: null, // Changed from empty string to null
    VIN: vin,
    USD: prices.USD,
    levelData: {
      expireDate: "",
      hotType: "",
      label: 0,
      level: 0
    },
    optionStyles: [],
    photoData: {
      all: Array.from({ length: 15 }, () => Math.floor(550000000 + Math.random() * 9000000)),
      count: 15,
      seoLinkB: `https://cdn4.riastatic.com/photosnew/auto/photo/${makeMeta.nameEng.toLowerCase()}_${modelMeta.nameEng.toLowerCase()}__${Math.floor(550000000 + Math.random() * 9000000)}b.jpg`,
      seoLinkF: `https://cdn4.riastatic.com/photosnew/auto/photo/${makeMeta.nameEng.toLowerCase()}_${modelMeta.nameEng.toLowerCase()}__${Math.floor(550000000 + Math.random() * 9000000)}f.jpg`,
      seoLinkM: `https://cdn4.riastatic.com/photosnew/auto/photo/${makeMeta.nameEng.toLowerCase()}_${modelMeta.nameEng.toLowerCase()}__${Math.floor(550000000 + Math.random() * 9000000)}m.jpg`,
      seoLinkSX: `https://cdn4.riastatic.com/photosnew/auto/photo/${makeMeta.nameEng.toLowerCase()}_${modelMeta.nameEng.toLowerCase()}__${Math.floor(550000000 + Math.random() * 9000000)}sx.jpg`
    },
    locationCityName: region.regionName,
    userPhoneData: {
      phone: "(xxx) xxx xx xx"
    },
    subCategoryName: subCategory.name,
    markNameEng: makeMeta.nameEng.toLowerCase(),
    modelId: modelMeta.id,
    exchangeTypeId: 3,
    deliveryStatus: 0,
    userBlocked: [],
    userId: Math.floor(10000000 + Math.random() * 9000000),
    stateData: {
      cityId: Math.floor(100 + Math.random() * 500),
      linkToCatalog: `/city/${region.regionNameEng.toLowerCase()}/`,
      name: region.name,
      regionName: region.regionName,
      regionNameEng: region.regionNameEng.toLowerCase(),
      stateId: region.id,
      title: `Пошук оголошень по місту ${region.regionName}`
    },
    dealer: {
      id: 0,
      isReliable: false,
      link: "",
      logo: "",
      name: "",
      packageId: 0,
      type: "",
      typeId: 0,
      verified: false
    },
    markName: makeMeta.name,
    realtyExchange: false,
    plateNumber: `AA ${Math.floor(1000 + Math.random() * 9000)} AA`,
    autoData: {
      active: true,
      autoId: autoId, // This is now guaranteed to be an integer
      bodyId: bodyType.id,
      categoryId: category.id,
      categoryNameEng: category.nameEng.toLowerCase(),
      custom: Math.random() > 0.5 ? 1 : 0,
      description: "Автомобіль у відмінному стані. Без ДТП. Один власник.",
      driveId: driveType.id,
      driveName: driveType.name,
      driveNameEng: driveType.nameEng.toLowerCase().replace(/-/g, ''),
      equipmentId: null,
      fromArchive: false,
      fuelId: fuelType.id,
      fuelName: `${fuelType.name}, ${(Math.floor(10 + Math.random() * 30) / 10).toFixed(1)} л.`,
      fuelNameEng: fuelType.nameEng.toLowerCase().replace(/-/g, '-'),
      gearBoxId: gearboxType.id,
      gearboxName: gearboxType.name,
      gearboxNameEng: gearboxType.nameEng.toLowerCase(),
      generationId: null,
      isSold: false,
      mainCurrency: "USD",
      modificationId: null,
      onModeration: false,
      race: `${raceInteger} тис. км`,
      raceInt: raceInteger,
      statusId: 0,
      subCategoryNameEng: subCategory.nameEng,
      vat: false,
      version: "",
      withVideo: false,
      withVideoMessages: false,
      year: year
    },
    modelNameEng: modelMeta.nameEng.toLowerCase(),
    verifiedByInspectionCenter: false,
    canSetSpecificPhoneToAdvert: false,
    userHideADSStatus: false,
    partnerId: 0,
    auctionPossible: true,
    withInfoBar: false,
    haveInfotechReport: false,
    firstTime: true,
    badges: [],
    vinSvg: "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"130\" height=\"15\"><path fill=\"#414042\" d=\"M2.63 13.77L3.88 13.77L5.86 6.68C5.94 6.38 6.03 6.01 6.13 5.59C6.16 5.72 6.26 6.09 6.42 6.68L8.39 13.77L9.57 13.77L12.12 4.46L10.89 4.46L9.43 10.44C9.24 11.20 9.08 11.88 8.96 12.47C8.81 11.44 8.57 10.34 8.24 9.17L6.91 4.46L5.42 4.46L3.65 10.75C3.61 10.90 3.47 11.47 3.23 12.47C3.12 11.84 2.99 11.20 2.84 10.56L1.42 4.46L0.16 4.46Z\"/></svg>",
    autoInfoBar: {
      abroad: false,
      confiscatedCar: false,
      custom: fuelType.id === 2, // Only diesel cars are imported
      damage: false,
      onRepairParts: false,
      underCredit: false
    },
    isAutoAddedByPartner: false,
    addDate: addDate,
    sendComments: 0,
    codedVin: {
      iv: Math.random().toString(36).substring(2, 18),
      text: Math.random().toString(36).substring(2, 66)
    },
    secureKey: Math.random().toString(36).substring(2, 34),
    exchangeType: "Рівноцінний",
    color: color,
    checkedVin: {
      isShow: false,
      orderId: 0,
      vin: vin
    }
  };

  res.json(mockResponse);
});

app.get('/auto/search', (req, res) => {
  // Get requested count or default to 50 
  const requestedCount = parseInt(req.query.countpage) || 50;
  // Total count is usually more than what's shown on a page
  const totalCount = Math.floor(requestedCount * (1 + Math.random()));
  
  // Generate random car IDs
  const carIds = generateCarIds(Math.min(requestedCount, 50));
  
  // Create search result data with enhanced fields
  let resultData = carIds.map(id => {
    // Generate random selections for this entry
    const category = rand(categories);
    const gearboxType = rand(gearboxTypes);
    
    // Generate subcategory based on category
    const subCategories = {
      1: { id: 1, name: "Легкові", nameEng: "Cars" }, // Cars subcategories
      2: { id: 2, name: "Мотоцикли", nameEng: "Motorcycles" },
      3: { id: 3, name: "Човни", nameEng: "Boats" },
      // etc.
    };
    
    const subCategory = subCategories[category.id] || subCategories[1];
    
    return {
      id: id, // This will now be a string as returned by generateCarIds
      type: "UsedAuto",
      categoryId: category.id,
      categoryName: category.name,
      categoryNameEng: category.nameEng,
      subCategoryId: subCategory.id,
      subCategoryName: subCategory.name,
      subCategoryNameEng: subCategory.nameEng,
      gearBoxId: gearboxType.id,
      gearboxName: gearboxType.name,
      gearboxNameEng: gearboxType.nameEng
    };
  });
  
  // Insert special offer in position 2 (after the first two items)
  resultData.splice(2, 0, {
    id: "100500", // Use a string as expected by the API consumer
    type: "OfferOfTheDay",
    categoryId: 1,
    categoryName: "Легкові",
    categoryNameEng: "Cars",
    subCategoryId: 1,
    subCategoryName: "Легкові",
    subCategoryNameEng: "Cars",
    gearBoxId: 2,
    gearboxName: "Автомат",
    gearboxNameEng: "Automatic"
  });

  // Example values from the real response
  const bodystyle = [3, 2];
  const marka_id = [79, 84];
  const model_id = [null, null];
  const s_yers = [2010, 2012];
  const po_yers = [2017, 2016];
  const state = [1, 2, 10];
  const city = [];
  const auto_options = [477];
  const type = ["1", "2", null, "4", null, null, null, "8"];
  const gear_id = [1, 2, 3];
  const fuel_id = [1, 2, 4, 8];
  const country = [276, 392];

  const mockSearchResponse = {
    additional_params: {
      lang_id: 2,
      page: 0,
      view_type_id: 0,
      target: "search",
      section: "auto",
      catalog_name: "",
      elastica: true,
      nodejs: true,
      searchByTypeAction: true
    },
    result: {
      search_result: {
        ids: carIds, // These are now strings
        count: totalCount,
        last_id: 0
      },
      search_result_common: {
        count: totalCount,
        last_id: 0,
        data: resultData
      },
      active_marka: marka_id && marka_id.length > 0 ? {
        lang_id: 2,
        marka_id: marka_id[0],
        name: "Toyota",
        set_cat: "1,3,4,6,7,8",
        main_category: 1,
        active: 1,
        country_id: 392,
        eng: "toyota",
        count: 8986,
        fit: 0,
        count_newauto: 5265
      } : null,
      active_model: null,
      active_state: null,
      active_city: null,
      revies: null,
      isCommonSearch: true,
      additional: {
        user_auto_positions: [],
        search_params: {
          all: {
            category_id: 1,
            bodystyle: bodystyle,
            marka_id: marka_id,
            model_id: model_id,
            s_yers: s_yers,
            po_yers: po_yers,
            price_ot: 1000,
            price_do: 60000,
            currency: "1",
            auctionPossible: "1",
            state: state,
            city: city,
            abroad: false,
            custom: 0,
            auto_options: auto_options,
            type: type,
            engineVolumeFrom: 1.4,
            engineVolumeTo: 3.2,
            powerFrom: "90",
            powerTo: "250",
            power_name: "1",
            countpage: 50,
            with_photo: "1",
            searchType: 6,
            target: "search",
            event: "little",
            lang_id: 2,
            page: 0,
            limit_page: null,
            last_id: 0,
            saledParam: 0,
            mm_marka_filtr: [],
            mm_model_filtr: [],
            useOrigAutoTable: false,
            withoutStatus: false,
            with_video: false,
            under_credit: 0,
            confiscated_car: 0,
            exchange_filter: [],
            old_only: false,
            user_id: [],
            person_id: 0,
            with_discount: false,
            auto_id_str: "",
            black_user_id: 0,
            order_by: 0,
            is_online: false,
            withoutCache: false,
            with_last_id: false,
            top: 0,
            currency_id: 0,
            currencies_arr: [],
            hide_black_list: [],
            damage: 0,
            star_auto: 0,
            year: 0,
            auto_ids_search_position: 0,
            print_qs: 0,
            is_hot: 0,
            deletedAutoSearch: 0,
            can_be_checked: 0,
            excludeMM: [0, 0],
            generation_id: [null, null],
            modification_id: [null, null],
            gear_id: gear_id,
            fuel_id: fuel_id,
            country: country
          },
          cleaned: {
            category_id: 1,
            bodystyle: bodystyle,
            marka_id: marka_id,
            model_id: model_id,
            s_yers: s_yers,
            po_yers: po_yers,
            price_ot: 1000,
            price_do: 60000,
            currency: "1",
            auctionPossible: "1",
            state: state,
            city: city,
            auto_options: auto_options,
            type: type,
            engineVolumeFrom: 1.4,
            engineVolumeTo: 3.2,
            powerFrom: "90",
            powerTo: "250",
            power_name: "1",
            countpage: 50,
            with_photo: "1",
            searchType: 6,
            target: "search",
            event: "little",
            lang_id: 2,
            mm_marka_filtr: [],
            mm_model_filtr: [],
            exchange_filter: [],
            user_id: [],
            currencies_arr: [],
            hide_black_list: [],
            excludeMM: [0, 0],
            generation_id: [null, null],
            modification_id: [null, null],
            gear_id: gear_id,
            fuel_id: fuel_id,
            country: country
          }
        },
        query_string: req._parsedUrl ? req._parsedUrl.query : "category_id=1&bodystyle[0]=3&bodystyle[4]=2&marka_id[0]=79&model_id[0]=0&s_yers[0]=2010&po_yers[0]=2017&marka_id[1]=84&model_id[1]=0&s_yers[1]=2012&po_yers[1]=2016&brandOrigin[0]=276&brandOrigin[1]=392&price_ot=1000&price_do=60000&currency=1&auctionPossible=1&state[0]=1&city[0]=0&state[1]=2&city[1]=0&state[2]=10&city[2]=0&abroad=2&custom=1&auto_options[477]=477&type[0]=1&type[1]=2&type[3]=4&type[7]=8&gearbox[0]=1&gearbox[1]=2&gearbox[2]=3&engineVolumeFrom=1.4&engineVolumeTo=3.2&powerFrom=90&powerTo=250&power_name=1&countpage=50&with_photo=1"
      }
    }
  };

  res.json(mockSearchResponse);
});

app.listen(port, () => {
  console.log(`🚗 Mock Car API running at http://localhost:${port}`);
});
